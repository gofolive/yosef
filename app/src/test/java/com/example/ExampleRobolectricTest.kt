package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DietaryType
import com.example.data.model.DiningHall
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus
import com.example.data.repository.MealCardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MTU Digital Meal Card", appName)
    }

    @Test
    fun `dynamic token generation contains student ID and valid payload`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.MtuMealDatabase.getInstance(context)
        val repo = MealCardRepository(db.mealCardDao())

        val token = repo.generateDynamicToken("MTU/14022/13", MealSession.LUNCH)
        assertEquals("MTU/14022/13", token.mtuId)
        assertEquals(MealSession.LUNCH, token.session)
        assertTrue(token.tokenPayload.startsWith("MTU_MEAL::MTU/14022/13::LUNCH::"))
        assertTrue(token.remainingSeconds() > 0)

        val parsed = repo.parseDynamicToken(token.tokenPayload)
        assertNotNull(parsed)
        assertEquals("MTU/14022/13", parsed?.first)
        assertEquals(MealSession.LUNCH, parsed?.second)
    }
}
