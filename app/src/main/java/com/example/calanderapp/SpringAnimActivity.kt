package com.example.calanderapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.example.calanderapp.databinding.ActivitySpringAnimBinding

class SpringAnimActivity : AppCompatActivity() {

    lateinit var binding: ActivitySpringAnimBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpringAnimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val translateX = ObjectAnimator.ofFloat(binding.et1, "translationX", -1000f).apply {
            duration = 300
        }

        binding.start.setOnClickListener {

            AnimatorSet().apply {
                play(translateX)
                start()
            }.doOnEnd {
                val springAnim = SpringAnimation(binding.et2, SpringAnimation.TRANSLATION_X)
                val springForce = SpringForce()
                springForce.finalPosition = binding.et1.x
                springForce.stiffness = SpringForce.STIFFNESS_LOW
                springForce.dampingRatio = 0.5f
                springAnim.spring = springForce
                springAnim.start()
            }

        }

    }
}