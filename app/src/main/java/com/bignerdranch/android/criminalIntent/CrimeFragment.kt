package com.bignerdranch.android.criminalIntent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.*
import androidx.lifecycle.Observer
import java.io.File


private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID="crime_id"
private const val DIALOG_DATE="DialogDate"
private const val REQUEST_DATE=0
private const val REQUEST_CONTACT=1
private const val REQUEST_PHOTO=2
private const val DATE_FORMAT="EEE,MMM, dd"


class CrimeFragment : Fragment(),DatePickerFragment.Callbacks {

    private lateinit var crime:Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField:EditText
    private lateinit var dateButton:Button
    private lateinit var solvedCheckbox:CheckBox
    private lateinit var reportButton:Button
    private lateinit var suspectButton:Button
    private lateinit var photoButton:ImageButton
    private lateinit var photoView: ImageView

    private val crimeDetailViewModel:CrimeDetailViewModel by lazy{
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime=Crime()

        val crimeId:UUID=arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)




    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view=inflater.inflate(R.layout.fragment_crime,container,false)
        titleField=view.findViewById(R.id.crime_title) as EditText
        dateButton=view.findViewById(R.id.crime_date) as Button
        solvedCheckbox=view.findViewById(R.id.crime_solved) as CheckBox
        reportButton=view.findViewById(R.id.crime_report) as Button
        suspectButton=view.findViewById(R.id.crime_suspect) as Button
        photoButton=view.findViewById(R.id.crime_camera) as ImageButton
        photoView=view.findViewById(R.id.crime_photo) as ImageView

//        dateButton.apply{
//            text=crime.date.toString()
//            isEnabled=false
//        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime=crime
                    photoFile=crimeDetailViewModel.getPhotoFile(crime)
                    photoUri=FileProvider.getUriForFile(
                        requireActivity(),
                        "com.bignerdranch.android.criminalIntent.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text=crime.date.toString()
        solvedCheckbox.apply {
            isChecked=crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()){
            suspectButton.text=crime.suspect
        }
        updatePhotoView()
    }
    private fun updatePhotoView(){
        if (photoFile.exists()){
            val bitmap= getScaledBitmap(photoFile.path,requireActivity())
            photoView.setImageBitmap(bitmap)
        }else{
            photoView.setImageDrawable(null)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data !=null -> {
                val contactUri: Uri?=data.data
                val queryFields= arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor= contactUri?.let {
                    requireActivity().contentResolver
                        .query(it,queryFields,null,null,null)
                }
                cursor?.use {
                    if (it.count==0){
                        return
                    }
                    it.moveToFirst()
                    val suspect=it.getString(0)
                    crime.suspect=suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text=suspect
                }
            }
            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }



    private fun getCrimeReport():String{
        val solvedString=if (crime.isSolved){
            getString(R.string.crime_report_solved)
        }else{
            getString(R.string.crime_report_unsolved)
        }

        val dateString=DateFormat.format(DATE_FORMAT,crime.date).toString()

        val suspectString=if (crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        }else{
            getString(R.string.crime_report_suspect,crime.suspect)
        }

        return getString(R.string.crime_report,
            crime.title,dateString,solvedString,suspectString)


    }


    override fun onStart() {
        super.onStart()
        val titleWatcher=object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title=s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckbox.apply{
            setOnCheckedChangeListener{ _,isChecked ->
                crime.isSolved=isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
//            version with Material
//            val datePicker=MaterialDatePicker.Builder.datePicker()
//                .setTitleText(resources.getString(R.string.choose_date_for_crime_datepicker))
//                .build()
//
//            datePicker.addOnPositiveButtonClickListener { timeInMillis ->
//                val calendar=Calendar.getInstance()
//                calendar.timeInMillis=timeInMillis
//                calendar.add(Calendar.MONTH,1)
//                calendar.add(Calendar.DAY_OF_MONTH,1)
//
//                val dateFormat=SimpleDateFormat("MM.dd.yyyy")
//                val resultDate=dateFormat.format(calendar.time)
//
////                Snackbar.make(dateButton,resultDate,Snackbar.LENGTH_LONG).show()
//                crime.date=calendar.time
//            }
//
//            datePicker.show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT,getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT,getString(R.string.crime_report_subject))
            }.also{ intent ->

                val chooserIntent=Intent.createChooser(intent,getString(R.string.send_report))

                startActivity(chooserIntent)

            }
        }

        suspectButton.apply {
            var pickContactIntent:Intent=Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager:PackageManager=requireActivity().packageManager
            val resolvedActivity: ResolveInfo?=
                packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity==null){
                isEnabled=false
            }
        }

        photoButton.apply{
            val packageManager:PackageManager=requireActivity().packageManager

            val captureImage=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity:ResolveInfo?=
                packageManager.resolveActivity(captureImage,
                                        PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity==null){
                isEnabled=false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)

                val cameraActivities:List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities){
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    companion object{
        fun newInstance(crimeId: UUID):CrimeFragment{
            val args=Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }

    override fun onDateSelected(date: Date) {
        crime.date=date
        updateUI()
    }


}