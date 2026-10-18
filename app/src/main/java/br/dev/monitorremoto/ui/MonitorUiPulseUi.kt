package br.dev.monitorremoto.ui

import br.dev.monitorremoto.core.SettingsStore
import br.dev.monitorremoto.service.MonitorService
import br.dev.monitorremoto.transport.AuthenticatedAccess

/** Amostra leve para decidir se a View deve ser reconstruída. */
internal fun MainActivity.currentMonitorState() = MonitorUiRefreshPolicy.State(
    enabled = SettingsStore.isMonitorEnabled(this),
    serviceRunning = MonitorService.isRunning,
    serviceActive = MonitorService.isServiceActive,
    screenSharing = br.dev.monitorremoto.capture.ScreenFrameStore.sharing,
    cameraActive = br.dev.monitorremoto.capture.CameraFrameStore.isActive(),
    photoPrepared = br.dev.monitorremoto.service.PhotoReadyService.armed,
    address = preferredLocalAddress().orEmpty(),
    recentClients = AuthenticatedAccess.recent().map { "${it.key}|${it.name}|${it.method}" }
)

internal fun MainActivity.refreshMonitorIfChanged(): Boolean {
    if (mode != 0) return false
    if (!MonitorUiRefreshPolicy.needsRender(displayedMonitorState, currentMonitorState())) return false
    render()
    return true
}

/** Agrupa eventos de acessos muito próximos, sem acumular callbacks ao sair da tela. */
internal fun MainActivity.monitorAccessListener(refresh: Runnable): () -> Unit = {
    handler.removeCallbacks(refresh)
    handler.postDelayed(refresh, 180L)
}

/** Se houve render, render() já programou o próximo tick. Nunca agendar duas vezes. */
internal fun MainActivity.monitorStatusPoller(recover: () -> Unit): Runnable = object : Runnable {
    override fun run() {
        if (active && mode == 0) {
            if (SettingsStore.isMonitorEnabled(this@monitorStatusPoller) && !MonitorService.isServiceActive) recover()
            if (!refreshMonitorIfChanged()) handler.postDelayed(this, 5_000L)
        }
    }
}
