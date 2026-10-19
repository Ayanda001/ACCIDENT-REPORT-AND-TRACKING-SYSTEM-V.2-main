package com.example.arts.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.arts.R

class HelplinesFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_helplines, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }
        mapOf(
            R.id.btn_call_112 to "112", R.id.btn_call_10111 to "10111",
            R.id.btn_call_10177 to "10177", R.id.btn_call_17737 to "17737",
            R.id.btn_call_0800012322 to "0800012322", R.id.btn_call_traffic to "0861400800"
        ).forEach { (id, number) ->
            view.findViewById<View>(id)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        }
    }
}
