package dev.ewio.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object VCDB {
    private lateinit var ds: HikariDataSource

    fun init(dbFile: File) {
        dbFile.parentFile.mkdirs()
        val cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            maximumPoolSize = 1 // SQLite: 1 Writer / viele Reader bringen hier nichts
            poolName = "VisualClaim-SQLite"
            isAutoCommit = true
            connectionInitSql = """
                PRAGMA foreign_keys = ON;
                PRAGMA journal_mode = WAL;
                PRAGMA synchronous = NORMAL;
                PRAGMA cache_size = -20000;
            """.trimIndent()
        }
        ds = HikariDataSource(cfg)
        Database.connect(ds)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(VCPlayers, VCClaims, VCChunks)
        }
    }

    fun shutdown() {
        if (this::ds.isInitialized) ds.close()
    }
}