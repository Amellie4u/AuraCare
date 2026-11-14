package com.example.data.recipes

data class BrainHealthyRecipe(
    val id: String,
    val title: String,
    val mealCategory: String,
    val prepMinutes: Int,
    val dietTags: List<String>,
    val caloriesSummary: String,
    val benefitNote: String,
    val ingredients: List<String>,
    val steps: List<String>
)

object RecipeCatalog {
    val allRecipes: List<BrainHealthyRecipe> = listOf(
        BrainHealthyRecipe(
            id = "keto_salmon_avocado",
            title = "Wild Salmon, Avocado & Hemp Bowl",
            mealCategory = "Lunch",
            prepMinutes = 15,
            dietTags = listOf("Ketogenic", "Modified Atkins", "High-Omega3", "Low-Glycemic"),
            caloriesSummary = "540 kcal · 4g net carbs · 38g fat",
            benefitNote = "Rich in EPA/DHA omega-3 fatty acids and ketogenic fats that support neuronal membrane stability.",
            ingredients = listOf(
                "150g grilled wild salmon",
                "1/2 ripe avocado, sliced",
                "2 cups baby spinach & arugula",
                "1 tbsp shelled hemp seeds",
                "1.5 tbsp extra-virgin olive oil + lemon"
            ),
            steps = listOf(
                "Arrange baby spinach and arugula in a wide bowl.",
                "Top with warm flaky salmon and sliced avocado.",
                "Sprinkle hemp seeds and drizzle olive oil and fresh lemon juice."
            )
        ),
        BrainHealthyRecipe(
            id = "keto_chia_pudding",
            title = "Coconut Chia & Walnut Pudding",
            mealCategory = "Breakfast",
            prepMinutes = 10,
            dietTags = listOf("Ketogenic", "Modified Atkins", "Low-Glycemic"),
            caloriesSummary = "390 kcal · 3g net carbs · 32g fat",
            benefitNote = "Steady blood-glucose release with MCT fats from coconut milk and plant omega-3 from walnuts.",
            ingredients = listOf(
                "3 tbsp chia seeds",
                "180ml unsweetened full-fat coconut milk",
                "1/4 tsp pure vanilla & pinch of cinnamon",
                "2 tbsp crushed raw walnuts",
                "6 fresh raspberries"
            ),
            steps = listOf(
                "Whisk chia seeds, coconut milk, vanilla, and cinnamon.",
                "Chill 20 minutes or overnight until thickened.",
                "Top with crushed walnuts and raspberries before serving."
            )
        ),
        BrainHealthyRecipe(
            id = "lowgi_turkey_zoodles",
            title = "Herb Turkey & Zucchini Pesto Noodles",
            mealCategory = "Dinner",
            prepMinutes = 20,
            dietTags = listOf("Modified Atkins", "Low-Glycemic", "Balanced", "High-Protein"),
            caloriesSummary = "440 kcal · 7g net carbs · 34g protein",
            benefitNote = "Avoids refined-carb glucose spikes while delivering magnesium and B6 from basil and pine nuts.",
            ingredients = listOf(
                "160g lean ground turkey",
                "2 medium spiralized zucchinis",
                "2 tbsp basil olive-oil pesto",
                "6 cherry tomatoes, halved",
                "1 tbsp grated parmesan"
            ),
            steps = listOf(
                "Saute ground turkey in olive oil with garlic until cooked through.",
                "Toss in zucchini noodles for 2 minutes until tender-crisp.",
                "Remove from heat, fold in pesto and tomatoes, and finish with parmesan."
            )
        ),
        BrainHealthyRecipe(
            id = "med_berry_yogurt",
            title = "Greek Yogurt, Flax & Blueberry Parfait",
            mealCategory = "Breakfast",
            prepMinutes = 5,
            dietTags = listOf("Low-Glycemic", "Balanced", "Mediterranean"),
            caloriesSummary = "310 kcal · 14g net carbs · 20g protein",
            benefitNote = "Antioxidant anthocyanins and probiotic protein to support steady morning energy with medications.",
            ingredients = listOf(
                "170g plain full-fat Greek yogurt",
                "1/3 cup fresh blueberries",
                "1 tbsp ground flaxseed",
                "1 tbsp pumpkin seeds (magnesium-rich)",
                "Light drizzle of olive oil or almond butter"
            ),
            steps = listOf(
                "Spoon Greek yogurt into a chilled bowl.",
                "Layer with fresh blueberries, ground flaxseed, and pumpkin seeds.",
                "Stir gently and enjoy alongside morning hydration."
            )
        ),
        BrainHealthyRecipe(
            id = "snack_almond_celery",
            title = "Almond Butter & Hemp Celery Boats",
            mealCategory = "Snack",
            prepMinutes = 5,
            dietTags = listOf("Ketogenic", "Modified Atkins", "Low-Glycemic", "Balanced"),
            caloriesSummary = "220 kcal · 3g net carbs · 18g fat",
            benefitNote = "Quick, gentle snack to take alongside anti-seizure medications to reduce stomach upset.",
            ingredients = listOf(
                "3 crisp celery stalks",
                "2 tbsp unsweetened creamy almond butter",
                "1 tsp hemp hearts",
                "Pinch of sea salt"
            ),
            steps = listOf(
                "Fill washed celery stalks with almond butter.",
                "Sprinkle hemp hearts and a light pinch of sea salt."
            )
        ),
        BrainHealthyRecipe(
            id = "dinner_turmeric_cod",
            title = "Golden Turmeric Baked Cod & Broccoli",
            mealCategory = "Dinner",
            prepMinutes = 25,
            dietTags = listOf("Balanced", "Low-Glycemic", "Mediterranean", "Anti-Inflammatory"),
            caloriesSummary = "410 kcal · 9g net carbs · 36g protein",
            benefitNote = "Light, anti-inflammatory evening meal rich in vitamin B6, potassium, and easy-to-digest protein.",
            ingredients = listOf(
                "170g wild cod fillet",
                "1.5 cups broccoli florets",
                "1.5 tbsp olive oil",
                "1/2 tsp turmeric + black pepper + lemon zest",
                "1 tbsp toasted sliced almonds"
            ),
            steps = listOf(
                "Toss broccoli florets and cod in olive oil, turmeric, pepper, and lemon.",
                "Bake at 200C (400F) for 15–18 minutes until cod flakes easily.",
                "Top with toasted almonds and serve warm."
            )
        )
    )

    fun suggestedFor(dietaryPreference: String, mealFilter: String? = null): List<BrainHealthyRecipe> {
        val byMeal = if (mealFilter.isNullOrBlank() || mealFilter == "All") {
            allRecipes
        } else {
            allRecipes.filter { it.mealCategory.equals(mealFilter, ignoreCase = true) }
        }
        val pref = dietaryPreference.trim()
        if (pref.isBlank() || pref.equals("Balanced", ignoreCase = true)) {
            return byMeal
        }
        val matched = byMeal.filter { recipe ->
            recipe.dietTags.any { it.equals(pref, ignoreCase = true) }
        }
        val remaining = byMeal.filterNot { it in matched }
        return matched + remaining
    }
}
