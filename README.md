# Drive Assist

This is a android application for my dad. My dad has to answer calls when driving, and it is
dangerous. But Google Assistant is not available in China, so I think this application 
may help out. It is a fully hands free, voice controlled call maker using library CMU-Sphinx, 
PocketSphinx on Android specifically. 

https://cmusphinx.github.io/wiki/tutorialandroid/

NOTE: This project uses Mandarin Chinese, including voice dictionary, UI content, and tts voice. 

## Idea

Basically, this whole project is a keyword recognition state machine. Each keyword recognition 
process is a state, and is a class in the code. Every recognition process is inherited from 
either keyword recognition prototype or grammar recognition prototype. Although currently I use 
only keyword recognition, it is capable of easily adding new recognition processes and features.


I've constructed the project 
in the way that is easy to add new functionalities and features. But keyword recognition is prone
to error and 

## Setup & Running

Checkout this project from Android Studio, the IDE should download all dependencies.

After the app is installed and started up, it should take a while (roughly 30s) to start 
responding even if the main menu shows up. 

There will be a video demo link here.

Sorry, it is in Chinese.

