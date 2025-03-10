package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.FragmentActivity
import androidx.compose.desktop.runtime.fragment.Fragment
import androidx.compose.desktop.runtime.fragment.FragmentComposeView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.core.bundle.Bundle
import com.github.knightwood.example.components.SampleButton

class TestFragmentActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        register<Fragment1>()
        setContent {
            ComposeView() {
                MaterialTheme {
                    Column {
                        HorizontalDivider()
                        fragmentManager.get("123").screen()
                        HorizontalDivider()
                        SampleButton("显示隐藏"){
                            fragmentManager.get("123").run {
                                if (mVisibility.value){
                                    hide()
                                }else{
                                    show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class Fragment1 : Fragment() {
    init {
        mWho = "123"
    }

    override fun onCreateView(): FragmentComposeView? {
        return ComposeView {
            MaterialTheme {
                Column {
                    Text("界面1")
                }
            }
        }
    }
}
