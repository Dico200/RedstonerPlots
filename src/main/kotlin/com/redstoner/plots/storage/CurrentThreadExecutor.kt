package com.redstoner.plots.storage

import java.util.concurrent.Executor

class CurrentThreadExecutor : Executor {

    override fun execute(command: Runnable?) {
        command!!.run()
    }

}