package com.example.ibe.morse

import android.app.Activity
import android.content.Context
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

class MainActivity : AppCompatActivity() {

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
            R.id.action_settings -> true
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


}
