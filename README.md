# Drive Assist

This is a android application for my dad. My dad has to answer calls when driving, and it is
dangerous. But Google Assistant is not available in China, so I think this application 
may help out. It is a fully hands free, voice controlled call maker using library CMU-Sphinx, 
PocketSphinx on Android specifically. 

https://cmusphinx.github.io/wiki/tutorialandroid/

NOTE: This project uses Mandarin Chinese, including voice dictionary, UI content, and tts voice. 

## Programming Idea

Basically, this whole project is a keyword recognition state machine. Each keyword recognition 
process is a state, and is a class in the code. Every recognition process is inherited from 
either keyword recognition prototype or grammar recognition prototype. Although currently
it has only call making feature, it is capable of easily adding new recognition processes and features.

Each recognition process is responsible for processing its own recognition result, next state transition,
permission request (at setup time), changing text in UI, setting tts, etc. Each recognition process could also implement a confirmable 
interface so that it could go to a confirm recognition process and come back to do after confirm operations.

During setup time, recognition process classes are loaded according to assets/sync/load_list file.
Transiting to an unloaded recognition process will crash. Recognition process that is not fully 
implemented is safe as long as it doesn't go to a bizarre state. 

## Takeaway & Next Step

I've constructed the project in the way that is easy to add new functionalities and features. 
But in general, keyword recognition is prone to error, and kinda not user-friendly. Setting
 threshold for each keyword is also very tedious and inflexible. So recognizing 
a sentence and understanding command from the sentence is a better approach, which is also
closer to the idea of a voice assistant.

Also, telling phone numbers fast is a big big trouble, especially repeatative numbers. Mixed up phonetics 
will make continuous phone number recognition almost never right.

CMU-Sphinx uses Hidden Markov Model for recognizing process, but it is proven that Neural Network
reinfored HMM could perform better. So it is my next objective. With sentences, language model is almost a must, 
which could tell which word is more likely to appear after.

## Setup & Running

Checkout this project from Android Studio, the IDE should download all dependencies.

After the app is installed and started up, it should take a while (roughly 30s) to start 
responding even if the main menu shows up. 

There will be a video demo link here.

Sorry, it is in Chinese.
