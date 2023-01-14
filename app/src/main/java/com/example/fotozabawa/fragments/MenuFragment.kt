package com.example.fotozabawa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import com.example.fotozabawa.databinding.FragmentMenuBinding
import androidx.fragment.app.Fragment
import com.example.fotozabawa.database.AppDatabase
import com.example.fotozabawa.R
import com.example.fotozabawa.model.Ustawienia
import kotlinx.coroutines.*


class MenuFragment : Fragment() {
    private lateinit var appDatabase: AppDatabase
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private val tryby = arrayOf("1 zdjęcie", "2 zdjęcia", "3 zdjęcia", "6 zdjęć")
    private val czas = arrayOf("1 sekunda", "3 sekundy", "5 sekund", "10 sekund")
    private val piosenka = arrayOf("1 piosenka","2 piosenka","3 piosenka","4 piosenka")
    private var tryb_number = 0
    private var czas_number = 0
    private var tryb_position = 0
    private var czas_position = 0
    private var banner_selected = "space"
    private var piosenka_selected = 0
    private var piosenka_position = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val img1 = view.findViewById<ImageView>(R.id.imageView_baner1)
        img1.setImageResource(R.drawable.space)
        img1.setOnClickListener{
            banner_selected = "space"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }
        val img2 = view.findViewById<ImageView>(R.id.imageView_baner2)
        img2.setImageResource(R.drawable.megamind)
        img2.setOnClickListener{
            banner_selected = "megamind"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }
        val img3 = view.findViewById<ImageView>(R.id.imageView_baner3)
        img3.setImageResource(R.drawable.space)
        img3.setOnClickListener{banner_selected = "space"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }
        val img4 = view.findViewById<ImageView>(R.id.imageView_baner4)
        img4.setImageResource(R.drawable.space)
        img4.setOnClickListener{banner_selected = "space"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }
        val img5 = view.findViewById<ImageView>(R.id.imageView_baner5)
        img5.setImageResource(R.drawable.space)
        img5.setOnClickListener{banner_selected = "space"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }
        val img6 = view.findViewById<ImageView>(R.id.imageView_baner6)
        img6.setImageResource(R.drawable.space)
        img6.setOnClickListener{banner_selected = "space"
            Toast.makeText(requireContext(), "Wybrano baner: "+banner_selected, Toast.LENGTH_SHORT).show()
        }



        //----------------------lista--------------------
        val spinner_tryb = view.findViewById<Spinner>(R.id.spinner_tryb)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tryby)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_tryb.adapter = adapter
        spinner_tryb.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinner_tryb.setSelection(position)
                spinner_tryb.setPrompt(tryby[position])
                tryb_number=tryby[position].subSequence(0,1).toString().toInt()
                tryb_position=position

            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val spinner_czas = view.findViewById<Spinner>(R.id.spinner_czas)
        val adapter2 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, czas)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_czas.adapter = adapter2
        spinner_czas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinner_czas.setSelection(position)
                spinner_czas.setPrompt(czas[position])
                czas_number=czas[position].subSequence(0,1).toString().toInt()
                czas_position=position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val spinner_piosenka = view.findViewById<Spinner>(R.id.spinner_piosenka)
        val adapter3 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, piosenka)
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_piosenka.adapter = adapter3
        spinner_piosenka.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinner_piosenka.setSelection(position)
                spinner_piosenka.setPrompt(piosenka[position])
                piosenka_selected=piosenka[position].subSequence(0,1).toString().toInt()
                piosenka_position=position
                Toast.makeText(requireContext(), piosenka[position], Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        runBlocking(Dispatchers.IO) {
            czas_position = appDatabase.ustawieniaDao().getCzas_position()
            tryb_position = appDatabase.ustawieniaDao().getTryb_position()
        }
        spinner_czas.setSelection(czas_position)
        spinner_tryb.setSelection(tryb_position)
        //----------------------powrót z menu------------------

        val myButton = view.findViewById<Button>(R.id.button_start)
        myButton.setOnClickListener{
            var ustawienie = Ustawienia(0,0,0,"space",0)

            runBlocking(Dispatchers.IO) {
                appDatabase.ustawieniaDao().deleteAll()
                ustawienie = Ustawienia(czas_number,czas_position,tryb_number,banner_selected,tryb_position)
                appDatabase.ustawieniaDao().insert(ustawienie)
            }

            val fragment : Fragment = StronaGlownaFragment()
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frameLayout, fragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
            requireActivity().title = "Strona Główna"
        }
    }
}