package com.example.foodapp

import android.os.Bundle
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.foodapp.theme.FoodAppTheme
import com.example.foodapp.ui.navigation.FoodAppRoot
import com.example.foodapp.utils.NfcManager
import com.example.foodapp.utils.NfcReaderHelper

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        enableEdgeToEdge()
        setContent {
            FoodAppTheme { 
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
                    FoodAppRoot() 
                } 
            }
        }
        
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null) {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            nfcAdapter?.enableReaderMode(this, this, 
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, 
                options)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: android.nfc.Tag) {
        if (!NfcManager.isListening) return
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            val card = NfcReaderHelper.parseCard(isoDep)
            if (card != null) {
                runOnUiThread {
                    NfcManager.emitCard(card)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "icelandapp" && data.host == "safepay") {
            when (data.path) {
                "/success" -> {
                    val tracker = data.getQueryParameter("tracker") ?: ""
                    val reference = data.getQueryParameter("reference") ?: ""
                    com.example.foodapp.utils.SafepayCallbackManager.emitSuccess(tracker, reference)
                }
                "/cancel" -> {
                    com.example.foodapp.utils.SafepayCallbackManager.emitCancelled()
                }
            }
        }
    }
}
