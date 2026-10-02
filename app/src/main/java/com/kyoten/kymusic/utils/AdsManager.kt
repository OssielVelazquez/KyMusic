package com.kyoten.kymusic.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdsManager {

    // IDs de prueba (para desarrollo) - Reemplázalos con los tuyos en producción
    private const val BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            MobileAds.initialize(context)
            isInitialized = true
            Log.d("AdsManager", "✅ AdMob inicializado")
        }
    }

    // En AdsManager.kt - Actualiza la función loadInterstitialAd()

    fun loadInterstitialAd(context: Context) {
        try {
            // ✅ Para API 36, usar el nuevo método de carga si está disponible
            val adRequest = AdRequest.Builder().build()

            // ✅ Para API 36, verificar que el contexto es válido
            if (context.applicationContext != null) {
                InterstitialAd.load(
                    context,
                    INTERSTITIAL_AD_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            Log.d("AdsManager", "✅ Interstitial cargado")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                            Log.e("AdsManager", "❌ Error: ${error.message}")
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("AdsManager", "Error cargando anuncio", e)
        }
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit = {}) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdsManager", "📢 Interstitial mostrado")
                }
            }
            ad.show(activity)  // ← Aquí se pasa el Activity
        } ?: run {
            onDismissed()
        }
    }

    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("AdsManager", "✅ Rewarded ad cargado")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e("AdsManager", "❌ Error cargando rewarded ad: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit = {}) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdsManager", "📢 Rewarded ad mostrado")
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d("AdsManager", "🎁 Recompensa: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            }
        } ?: run {
            Log.d("AdsManager", "⚠️ No hay rewarded ad cargado")
        }
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null
    fun isRewardedReady(): Boolean = rewardedAd != null
}