package com.example.pupbuddym.ui.main

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.pupbuddym.dto.Dog
import com.example.pupbuddym.dto.HotSpot
import com.example.pupbuddym.dto.Photo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

class MainViewModel : ViewModel() {
    val photos: ArrayList<Photo> = ArrayList<Photo>()
    private var _dogs: MutableLiveData<ArrayList<Dog>> = MutableLiveData<ArrayList<Dog>>()
    private lateinit var firestore : FirebaseFirestore
    private var storageReference = FirebaseStorage.getInstance().getReference()

    init {
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()
        listenToHouses()
    }

    fun saveSpot(hotSpot: HotSpot){
        val document = firestore.collection("hotSpot").document()
        hotSpot.hotSpotId = document.id
        val handle = document.set(hotSpot)
        handle.addOnSuccessListener { Log.d("Firebase", "Document Saved") }
        handle.addOnFailureListener { Log.d("Firebase", "Save failed $it") }
    }


    /**
     * This will hear any updates from Firestore
     */
    private fun listenToHouses() {
        firestore.collection("houses").addSnapshotListener {
                snapshot, e ->
            // if there is an exception we want to skip.
            if (e != null) {
                Log.w(TAG, "Listen Failed", e)
                return@addSnapshotListener
            }
            // if we are here, we did not encounter an exception
            if (snapshot != null) {
                // now, we have a populated snapshot
                val allDogs = ArrayList<Dog>()
                val documents = snapshot.documents
                documents.forEach {

                    val dog = it.toObject(Dog::class.java)
                    if (dog != null) {
                        dog.dogId = it.id
                        allDogs.add(dog!!)
                    }
                }
                _dogs.value = allDogs
            }
        }
    }

    fun save(
        dog: Dog,
        photos: java.util.ArrayList<Photo>
    ) {
        val document =
            if (dog.dogId != null && dog.dogId.isNotEmpty()) {
                // updating existing
                firestore.collection("houses")
                    .document(dog.houseId)
                    .collection("dogs")
                    .document(dog.dogId)
            } else {
                // create new
                firestore.collection("houses")
                    .document(dog.houseId)
                    .collection("dogs")
                    .document()
            }
        dog.dogId = document.id
        val set = document.set(dog)
        set.addOnSuccessListener {
            Log.d("Firebase", "document saved")
            if (photos != null && photos.size > 0) {
                savePhotos(dog, photos)
            }

        }
        set.addOnFailureListener {
            Log.d("Firebase", "Save Failed")
        }
    }

    private fun savePhotos(
        dog: Dog,
        photos: java.util.ArrayList<Photo>
    ) {
        val collection = firestore.collection("houses")
            .document(dog.houseId)
            .collection("dogs")
            .document(dog.dogId)
            .collection("photos")
        photos.forEach {
                photo -> val task = collection.add(photo)
            task.addOnSuccessListener {
                photo.id = it.id
                uploadPhotos(dog, photos)
            }
        }
    }

    private fun uploadPhotos(dog: Dog, photos: ArrayList<Photo>) {
        photos.forEach {
                photo ->
            var uri = Uri.parse(photo.localUri)
            val imageRef = storageReference.child("images/${dog.dogId}/${uri.lastPathSegment}")
            val uploadTask = imageRef.putFile(uri)
            uploadTask.addOnSuccessListener {
                val downloadUrl = imageRef.downloadUrl
                downloadUrl.addOnSuccessListener {
                    photo.remoteUri = it.toString()
                    // update our Cloud Firestore with the public image URI.
                    updatePhotoDatabase(dog, photo)
                }

            }
            uploadTask.addOnFailureListener {
                Log.e(TAG, it.message ?: "No message")
            }
        }
    }

    private fun updatePhotoDatabase(dog: Dog, photo: Photo) {
        var photoCollection = firestore.collection("houses")
            .document(dog.houseId)
            .collection("dogs")
            .document(dog.dogId)
            .collection("photos")
        var handle = photoCollection.add(photo)
        handle.addOnSuccessListener {
            Log.i(TAG, "Successfully updated photo metadata")
            photo.id = it.id
            firestore.collection("houses")
                .document(dog.houseId)
                .collection("dogs")
                .document(dog.dogId)
                .collection("photos")
                .document(photo.id)
                .set(photo)
        }
    }

    internal var plants:MutableLiveData<ArrayList<Dog>>
        get() {return _dogs}
        set(value) {_dogs = value}
}