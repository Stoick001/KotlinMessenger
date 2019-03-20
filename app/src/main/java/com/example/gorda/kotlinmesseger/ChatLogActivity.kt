package com.example.gorda.kotlinmesseger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.security.spec.ECField

class ChatLogActivity : AppCompatActivity() {

    val adapter = GroupAdapter<ViewHolder>()

    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

        supportActionBar?.title = toUser?.username

        recyclerView_chatLog.adapter = adapter

        listenForMessages()

        send_button_ChatLog.setOnClickListener {
            performSendMessage()
        }
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object: ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val message = p0.getValue(ChatMessage::class.java)

                if (message != null) {
                    if (message.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessagesActivity.currentUser
                        adapter.add(ChatFromItem(message.text, currentUser!!))
                    } else {
                        adapter.add(ChatToItem(message.text, toUser!!))
                    }
                }

                recyclerView_chatLog.scrollToPosition(adapter.itemCount-1)
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }

        })
    }

    class ChatMessage(val id: String, val text:String, val fromId: String, val toId: String, val timestamp: Long) {
        constructor(): this("", "", "", "", -1)
    }

    private fun performSendMessage() {
        val text = sendMessage_editText_chatLog.text.toString()
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

        val fromId = FirebaseAuth.getInstance().uid!!
        val toId = user.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(
            reference.key!!,
            text,
            fromId,
            toId,
            System.currentTimeMillis()
        )

        reference.setValue(chatMessage)
            .addOnSuccessListener {
                sendMessage_editText_chatLog.text.clear()
                recyclerView_chatLog.scrollToPosition(adapter.getItemCount()-1)
            }

        val latestMessageReference = FirebaseDatabase.getInstance().getReference("latest-messages/$fromId/$toId")
        latestMessageReference.setValue(chatMessage)

        if (fromId != toId) {
            val latestMessageToReference = FirebaseDatabase.getInstance().getReference("latest-messages/$toId/$fromId")
            toReference.setValue(chatMessage)
            latestMessageToReference.setValue(chatMessage)
        }
    }
}

class ChatFromItem(val text: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.messageFrom_textView_chatLog.text = text

        val uri = user.profileImage
        try {
            Picasso.get().load(uri).into(viewHolder.itemView.profileFrom_imageView_chatLog)
        } catch(e:Exception) {
            e.printStackTrace()
        }

    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

}

class ChatToItem(val text: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.messageTo_textView_chatLog.text = text

        val uri = user.profileImage
        try {
            Picasso.get().load(uri).into(viewHolder.itemView.profileTo_imageView_chatLog)
        } catch(e:java.lang.Exception) {
            e.printStackTrace()
        }

    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

}