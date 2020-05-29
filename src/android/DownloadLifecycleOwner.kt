package jp.rabee

import androidx.lifecycle.*

class DownloadLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle() = lifecycleRegistry

    fun start() {
        // STARTEDの状態にしてLiveDataをActiveにする
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        // CREATEDの状態にしてLiveDataをInactiveにする
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
}