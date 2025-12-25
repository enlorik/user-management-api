package com.empress.usermanagementapi.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that database indexes are actually created in the database
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true"
})
public class DatabaseIndexIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testDatabaseIndexesAreCreated() throws Exception {
        // Execute a native query to trigger schema creation
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
        
        // Get database metadata
        var session = entityManager.unwrap(org.hibernate.Session.class);
        session.doWork(connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check User table indexes
            Set<String> userIndexes = new HashSet<>();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "USERS", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        userIndexes.add(indexName.toUpperCase() + ":" + columnName.toUpperCase());
                    }
                }
            }
            
            // Verify username and email indexes exist
            boolean hasUsernameIndex = userIndexes.stream()
                .anyMatch(idx -> idx.contains("USERNAME"));
            boolean hasEmailIndex = userIndexes.stream()
                .anyMatch(idx -> idx.contains("EMAIL"));
                
            assertTrue(hasUsernameIndex, 
                "User table should have index on username column. Found indexes: " + userIndexes);
            assertTrue(hasEmailIndex, 
                "User table should have index on email column. Found indexes: " + userIndexes);
            
            // Check PasswordResetToken table indexes
            Set<String> passwordResetTokenIndexes = new HashSet<>();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "PASSWORD_RESET_TOKEN", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        passwordResetTokenIndexes.add(indexName.toUpperCase() + ":" + columnName.toUpperCase());
                    }
                }
            }
            
            boolean hasPasswordResetTokenIndex = passwordResetTokenIndexes.stream()
                .anyMatch(idx -> idx.contains("TOKEN"));
            assertTrue(hasPasswordResetTokenIndex, 
                "PasswordResetToken table should have index on token column. Found indexes: " + passwordResetTokenIndexes);
            
            // Check EmailVerificationToken table indexes
            Set<String> emailVerificationTokenIndexes = new HashSet<>();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "EMAIL_VERIFICATION_TOKEN", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        emailVerificationTokenIndexes.add(indexName.toUpperCase() + ":" + columnName.toUpperCase());
                    }
                }
            }
            
            boolean hasEmailVerificationTokenIndex = emailVerificationTokenIndexes.stream()
                .anyMatch(idx -> idx.contains("TOKEN"));
            assertTrue(hasEmailVerificationTokenIndex, 
                "EmailVerificationToken table should have index on token column. Found indexes: " + emailVerificationTokenIndexes);
        });
    }
}
