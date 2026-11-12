package dev.base.app.feature.payments.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SharedPreferences en memoria (frontera de disco). Cuenta `commit()` y `apply()`. */
private class InMemoryPrefs : SharedPreferences {
    val data = mutableMapOf<String, Any?>()
    var commits = 0
    var applies = 0

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? = data[key] as String? ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as Set<String>?)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = data[key] as Int? ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = data[key] as Long? ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = data[key] as Float? ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as Boolean? ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) =
        Unit

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }

        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { changes[key] = values?.toSet() }

        override fun putInt(key: String, value: Int) = apply { changes[key] = value }

        override fun putLong(key: String, value: Long) = apply { changes[key] = value }

        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }

        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }

        override fun remove(key: String) = apply { removals += key }

        override fun clear() = apply { clear = true }

        private fun write() {
            if (clear) data.clear()
            removals.forEach { data.remove(it) }
            changes.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
        }

        override fun commit(): Boolean {
            commits++
            write()
            return true
        }

        override fun apply() {
            applies++
            write()
        }
    }
}

class PrefsPendingPaymentStoreTest {
    private val prefs = InMemoryPrefs()
    private val context = mockk<Context> {
        every { getSharedPreferences(any(), any()) } returns prefs
    }

    private fun store() = PrefsPendingPaymentStore(context)

    // Cubre: pay_payment/R14
    @Test
    fun `usa el archivo pending_payments privado`() {
        store()

        verify { context.getSharedPreferences("pending_payments", Context.MODE_PRIVATE) }
    }

    // Cubre: pay_payment/R14
    @Test
    fun `save guarda la llave con commit sincrono`() {
        val store = store()

        store.save("p1", "k1")

        assertEquals(mapOf("p1" to "k1"), store.all())
        assertTrue("debe usar commit()", prefs.commits >= 1)
        assertEquals("no debe usar apply() (asíncrono)", 0, prefs.applies)
    }

    // Cubre: pay_payment/R14
    @Test
    fun `remove borra solo ese pago con commit sincrono`() {
        val store = store()
        store.save("p1", "k1")
        store.save("p2", "k2")
        val commitsBefore = prefs.commits

        store.remove("p1")

        assertEquals(mapOf("p2" to "k2"), store.all())
        assertTrue(prefs.commits > commitsBefore)
        assertEquals(0, prefs.applies)
    }

    // Cubre: pay_payment/R14
    @Test
    fun `save del mismo pago reemplaza la llave`() {
        val store = store()
        store.save("p1", "k1")
        store.save("p1", "k2")

        assertEquals(mapOf("p1" to "k2"), store.all())
    }

    // Cubre: pay_payment/R15
    @Test
    fun `las llaves sobreviven a una nueva instancia sobre el mismo disco`() {
        store().save("p1", "k1")
        store().save("p2", "k2")

        assertEquals(mapOf("p1" to "k1", "p2" to "k2"), store().all())
    }

    // Cubre: pay_payment/R15
    @Test
    fun `sin llaves all es vacio`() {
        assertTrue(store().all().isEmpty())
    }
}
