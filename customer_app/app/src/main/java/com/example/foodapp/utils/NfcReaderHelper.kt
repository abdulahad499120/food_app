package com.example.foodapp.utils

import android.nfc.tech.IsoDep
import android.util.Log
import com.github.devnied.emvnfccard.model.EmvCard
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.github.devnied.emvnfccard.parser.IProvider

class NfcProvider(private val mTagCom: IsoDep) : IProvider {
    override fun transceive(pCommand: ByteArray): ByteArray? {
        return try {
            mTagCom.transceive(pCommand)
        } catch (e: Exception) {
            null
        }
    }

    override fun getAt(): ByteArray {
        return mTagCom.historicalBytes ?: ByteArray(0)
    }
}

object NfcReaderHelper {
    fun parseCard(isoDep: IsoDep): EmvCard? {
        return try {
            isoDep.connect()
            val provider = NfcProvider(isoDep)
            val config = EmvTemplate.Config()
                .setContactLess(true)
                .setReadAllAids(true)
                .setReadTransactions(false)
                .setReadCplc(false)
                .setRemoveDefaultParsers(false)
                .setReadAt(true)
                
            val parser = EmvTemplate.Builder()
                .setProvider(provider)
                .setConfig(config)
                .build()
                
            val card = parser.readEmvCard()
            isoDep.close()
            card
        } catch (e: Exception) {
            Log.e("NfcReader", "Failed to parse NFC card", e)
            try { isoDep.close() } catch (ignored: Exception) {}
            null
        }
    }
}
