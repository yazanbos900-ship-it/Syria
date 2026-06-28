package com.example.domain.utils

import com.example.domain.model.Store

object StoreReputationCalculator {

    fun calculateReputationScore(
        store: Store,
        viewsCount: Int = 0,
        buyNowClicks: Int = 0,
        chatInquiries: Int = 0,
        productSaves: Int = 0,
        reviewsCount: Int = 0,
        recentProductsCount: Int = 0
    ): Double {
        // 1. Followers: 15 points per follower
        val followerPoints = store.followersCount * 15.0

        // 2. Rating: rating * 40 points (max 200 points)
        val ratingPoints = store.rating * 40.0

        // 3. Verification: +150 points if verified
        val verificationPoints = if (store.isVerified || store.verificationStatus == "Verified") 150.0 else 0.0

        // 4. Subscription tier weight: Starter (+10), Growth (+100), Pro (+300)
        val subscriptionPoints = when (store.subscriptionTier.lowercase()) {
            "pro" -> 300.0
            "growth" -> 100.0
            else -> 10.0
        }

        // 5. Engagement points:
        // Product/Store views: 1 point each
        // Buy Now clicks: 10 points each
        // Chat inquiries: 15 points each
        // Product saves: 5 points each
        val engagementPoints = (viewsCount * 1.0) + (buyNowClicks * 10.0) + (chatInquiries * 15.0) + (productSaves * 5.0)

        // 6. Reviews quantity: 15 points per completed review
        val reviewPoints = reviewsCount * 15.0

        // 7. Store activity: +20 points per recent product (max 100 points)
        val activityPoints = (recentProductsCount * 20.0).coerceAtMost(100.0)

        return followerPoints + ratingPoints + verificationPoints + subscriptionPoints + engagementPoints + reviewPoints + activityPoints
    }
}
