/*
 * Copyright (C) 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.chaldeaprjkt.gamespace.R

object UiTicks {

    private var soundPool: SoundPool? = null
    private var tickId = 0

    fun init(context: Context) {
        if (soundPool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()
        tickId = soundPool!!.load(context.applicationContext, R.raw.carousel_tick, 1)
    }

    fun tick() {
        if (tickId == 0) return
        soundPool?.play(tickId, 1f, 1f, 1, 0, 1f)
    }
}
