package com.crush.ads.admob.callback

import com.google.android.gms.ads.rewarded.RewardItem

open class RewardCallback {
    open fun onEarnedReward(rewardItem: RewardItem?){}
    open  fun onAdClosed(){}
    open  fun onAdFailedToShow(codeError: Int){}
    open  fun onAdImpression(){}
}