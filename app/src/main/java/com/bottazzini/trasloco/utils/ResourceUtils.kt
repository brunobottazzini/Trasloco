package com.bottazzini.trasloco.utils

import android.content.res.Resources

class ResourceUtils {
    companion object {
        fun getDrawableByName(resources: Resources, packageName: String, imageName: String) =
            resources.getIdentifier("drawable/$imageName", "id", packageName)
    }
}