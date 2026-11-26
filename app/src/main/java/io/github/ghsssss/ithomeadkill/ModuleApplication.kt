package io.github.ghsssss.ithomeadkill

import android.app.Application
import io.github.ghsssss.ithomeadkill.settings.FrameworkConnection

class ModuleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FrameworkConnection.start()
    }
}
