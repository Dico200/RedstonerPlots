package com.redstoner.plots.storage

data class DataStorageOptions(var address: String,
                              var database: String,
                              var username: String,
                              var password: String,
                              var poolSize: Int) {
}