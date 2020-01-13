package com.shibuiwilliam.firebase_tflite_arcore.ar

import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.shibuiwilliam.firebase_tflite_arcore.SceneformActivity
import com.shibuiwilliam.firebase_tflite_arcore.common.Constants
import kotlinx.android.synthetic.main.activity_sceneform.*

open class SceneformArFragment: ArFragment(){
    private val TAG = "SceneformArFragment"

    override fun getSessionConfiguration(session: Session?): Config {
        Log.i(TAG, "setup new session")
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)

        val config = Config(session)
        config.planeFindingMode = Constants.PLANE_FINDING_MODE
        config.updateMode = Constants.UPDATE_MODE
        session?.configure(config)
        this.arSceneView.setupSession(session)
        this.arSceneView.planeRenderer.isEnabled = Constants.PLANERENDERER_ENABLED
        this.arSceneView.planeRenderer.isVisible = Constants.PLANERENDERER_VISIBLE
        this.arSceneView.planeRenderer.isShadowReceiver = Constants.PLANERENDERER_SHADOW_RECEIVER

        if (session != null) {
            activity.let { it as? SceneformActivity }
        }
        Log.i(TAG, config.toString())
        return config
    }

}