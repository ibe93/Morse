package com.example.ibe.morse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.util.Log
import android.view.inputmethod.InputMethodManager
import org.json.JSONObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.concurrent.timerTask
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

val SAMPLE_RATE = 44100

class MainActivity : AppCompatActivity() {

    var prefs: SharedPreferences? = null

    private fun appendTextAndScroll(text: String) {
        if (mTextView != null) {
            mTextView.append(text + "\n")
            val layout = mTextView.getLayout()
            if (layout != null) {
                val scrollDelta = (layout!!.getLineBottom( mTextView.getLineCount() - 1) - mTextView.getScrollY() - mTextView.getHeight())
                if(scrollDelta > 0)
                    mTextView.scrollBy(0, scrollDelta)
            }
        }
    }

    val letToCodeDict : HashMap<String, String> = HashMap<String, String>()
    val codeToLetDict : HashMap<String, String> = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getDefaultSharedPreferences(this.applicationContext)
        val morsePitch = prefs!!.getString("morse_pitch", "550")
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        mTextView.movementMethod = ScrollingMovementMethod()
        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString())
            hideKeyboard()
        }

        val jsonObj = loadMorseJSONFile();
        buildDictsWithJSON(jsonObj)

        shwcodes.setOnClickListener{ _ ->
            mTextView.text = ""
            showCodes()
            hideKeyboard()
        }

        testButton.setOnClickListener { _ ->
            mTextView.text = ""
            val input = inputText.text.toString()

            appendTextAndScroll(input.toUpperCase())

            if (input.matches("(\\.|-|\\s/\\s|\\s)+".toRegex())) {
                val transMorse = translateMorse(input)
                appendTextAndScroll(transMorse.toUpperCase())
            }
            else {
                val transText = translateText(input)
                appendTextAndScroll(transText)
            }
            hideKeyboard()
        }

        soundBtn.setOnClickListener { _ ->
            val input = inputText.text.toString()
            playString(translateText(input),0)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
             val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun loadMorseJSONFile() : JSONObject {

        val filePath = "morse.json"

        val jsonStr = application.assets.open(filePath).bufferedReader().use { it.readText() }

        val jsonObj = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1))

        return jsonObj

    }


    private fun buildDictsWithJSON(jsonObj : JSONObject) {
        for ( key in jsonObj.keys() ) {
            val code : String = jsonObj[key] as String

            letToCodeDict.put(key,code)
            codeToLetDict.put(code,key)

            Log.d("log", "$key: $code")

        }
    }

    private fun showCodes() {
        appendTextAndScroll("HERE ARE THE CODES")
        for (key in letToCodeDict.keys.sorted()){
            appendTextAndScroll("${key.toUpperCase()}: ${letToCodeDict[key]}")
        }
    }

    private fun translateText(input : String) : String {

        var value = ""

        val lowerStr = input.toLowerCase()

        for (c in lowerStr) // Loop for checking all the input
        {
            // if space than explode
            if (c == ' ') value += "/ "
            else if (letToCodeDict.containsKey(c.toString())) value += "${letToCodeDict[c.toString()]} "
            else value += "? "
        }

        Log.d("log", "Morse: $value")

        return value

    }

    private fun translateMorse(input: String) : String {
        var value = ""

        val lowerStr = input.split("(\\s)+".toRegex())

        Log.d("log", "Split stirng: $lowerStr")

        for (item in lowerStr) {
            if (item == "/") value += " "
            else if (codeToLetDict.containsKey(item)) value += codeToLetDict[item]
            else value += "[NA]"
        }

        Log.d("log", "Text: $value")

        return value
    }

    fun playString(s:String, i: Int = 0) : Unit {
        if (i>s.length-1)
            return;
        var mDelay: Long = 0;

        var thenFun: () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {playString(s, i+1)})
        }

        var c = s[i]
        Log.d("Log", "Processing pos: " + i + " char: [" + c + "]")
        if (c=='.')
            playDot(thenFun)
        else if (c=='-')
            playDash(thenFun)
        else if (c=='/')
            pause(6*dotLength, thenFun)
        else if (c==' ')
            pause(2*dotLength, thenFun)
    }

    val dotLength:Int = 50
    val dashLength:Int = dotLength*3

    val dotSoundBuffer: ShortArray = genSineWaveSoundBuffer(550.0, dotLength) //freq: 550.0
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dashLength)

    fun playDash(onDone:()->Unit={}){
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer,{->pause(dotLength, onDone)})
    }
    fun playDot(onDone: () -> Unit={}){
        Log.d("DEBUG", "playDot")
        playSoundBuffer(dotSoundBuffer,{ -> pause(dotLength, onDone)})
    }

    fun pause(durationMSec:Int, onDone: () -> Unit={}){
        Log.d("DEBUG", "pause: ${durationMSec}")
        Timer().schedule(timerTask { onDone()  }, durationMSec.toLong())
    }

    private fun genSineWaveSoundBuffer(frequency:Double, durationMSec: Int):ShortArray{
        val duration : Int = Math.round((durationMSec/1000.0) * SAMPLE_RATE).toInt()

        var mSound: Double
        val mBuffer = ShortArray(duration)
        for(i in 0 until duration) {
            mSound= Math.sin(2.0*Math.PI*i.toDouble()/(SAMPLE_RATE/frequency))
            mBuffer[i] = (mSound*java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }

    private fun playSoundBuffer (mBuffer: ShortArray, onDone: () -> Unit={ }) {
        var minBufferSize = SAMPLE_RATE / 10
        if (minBufferSize < mBuffer.size) {
            minBufferSize = minBufferSize + minBufferSize *
                    (Math.round(mBuffer.size.toFloat()) / minBufferSize.toFloat()).toInt()
        }

        val nBuffer = ShortArray(minBufferSize)
        for (i in nBuffer.indices) {
            if (i < mBuffer.size)
                nBuffer[i] = mBuffer[i]
            else
                nBuffer[i] = 0
        }

        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM)

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.setNotificationMarkerPosition(mBuffer.size)
        mAudioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack){}
            override fun onMarkerReached(track: AudioTrack?) {
                Log.d("Log", "Audio track end of file reached...")
                mAudioTrack.stop()
                mAudioTrack.release()
                onDone()
            }
        })
        mAudioTrack.play()
        mAudioTrack.write(nBuffer, 0, minBufferSize)
    }


}
