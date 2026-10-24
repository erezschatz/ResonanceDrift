package net.erezschatz.resonancedrift.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/** A fresh in-memory [Db] per test. */
class DbTestRule : ExternalResource() {
    lateinit var db: Db
        private set

    override fun before() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), Db::class.java).build()
    }

    override fun after() {
        db.close()
    }
}
