package com.empress.usermanagementapi.entity;

import jakarta.persistence.Table;
import jakarta.persistence.Index;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that database indexes are properly configured on entities
 */
public class EntityIndexTest {

    @Test
    public void testUserEntityHasIndexes() {
        Table tableAnnotation = User.class.getAnnotation(Table.class);
        assertNotNull(tableAnnotation, "User entity should have @Table annotation");
        
        Index[] indexes = tableAnnotation.indexes();
        assertNotNull(indexes, "User entity should have indexes defined");
        assertEquals(2, indexes.length, "User entity should have 2 indexes");
        
        // Check for username index
        boolean hasUsernameIndex = false;
        boolean hasEmailIndex = false;
        
        for (Index index : indexes) {
            if (index.columnList().equals("username")) {
                hasUsernameIndex = true;
                assertEquals("idx_user_username", index.name(), "Username index should have correct name");
            }
            if (index.columnList().equals("email")) {
                hasEmailIndex = true;
                assertEquals("idx_user_email", index.name(), "Email index should have correct name");
            }
        }
        
        assertTrue(hasUsernameIndex, "User entity should have index on username column");
        assertTrue(hasEmailIndex, "User entity should have index on email column");
    }

    @Test
    public void testPasswordResetTokenEntityHasIndex() {
        Table tableAnnotation = PasswordResetToken.class.getAnnotation(Table.class);
        assertNotNull(tableAnnotation, "PasswordResetToken entity should have @Table annotation");
        
        Index[] indexes = tableAnnotation.indexes();
        assertNotNull(indexes, "PasswordResetToken entity should have indexes defined");
        assertEquals(1, indexes.length, "PasswordResetToken entity should have 1 index");
        
        Index index = indexes[0];
        assertEquals("idx_password_reset_token", index.name(), "PasswordResetToken index should have correct name");
        assertEquals("token", index.columnList(), "PasswordResetToken index should be on token column");
    }

    @Test
    public void testEmailVerificationTokenEntityHasIndex() {
        Table tableAnnotation = EmailVerificationToken.class.getAnnotation(Table.class);
        assertNotNull(tableAnnotation, "EmailVerificationToken entity should have @Table annotation");
        
        Index[] indexes = tableAnnotation.indexes();
        assertNotNull(indexes, "EmailVerificationToken entity should have indexes defined");
        assertEquals(1, indexes.length, "EmailVerificationToken entity should have 1 index");
        
        Index index = indexes[0];
        assertEquals("idx_email_verification_token", index.name(), "EmailVerificationToken index should have correct name");
        assertEquals("token", index.columnList(), "EmailVerificationToken index should be on token column");
    }
}
