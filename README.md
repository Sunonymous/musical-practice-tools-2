# Musical Practice Tools 2

## What is it?
This web application contains a handful of open-ended tools intended to help you practice playing or exploring music in a more systematic, organized manner. It includes a variety of tools which may be used on their own or together. A metronome is built into the application and each tool can be synchronized to automatically trigger new generations at certain intervals if desired. The tools include:

### Toggler
Given a series of options, the toggler will pick either a random (new) option out of its selection, or toggle between two options if only provided with two. Useful for practicing alterations of musical dynamics.
#### Example Uses
- Practice arpeggio runs, "toggling" between upward and downward on your instrument.
- Choose a dynamic to toggle between variations, eg. legato and staccato.
- Alternate between various levels of volume.

### Sequencer
The Sequencer is designed to create short, randomized numerical sequences. The tool contains the following settings:
- Highest and lowest values to appear in the sequence.
- The length of the sequence.
- The maximum number of duplicate values which can appear of any given number in the sequence.
- The characters used in between the chosen numbers.
- Whether or not to displays Roman Numerals instead of numbers (for scale degrees).
- Whether or not to used mixed case with Roman Numerals (sometimes used to differentiate between major and minor chords).
#### Example Uses
- Practice intervals in a musical scale by jumping through the sequences generated.
- Practice chord progressions by playing the key intervals suggested by the sequence.
- Make exotic arpeggios.

### Key
The Twelve Keys tool helps the user keep track of which keys they have practiced a particular drill in. Keys can be marked complete, excluded from practice, or switched between at will using the various controls. At this time, the tool only selects a new key randomly out of the incomplete keys.
#### Example Uses
- Practice a piece or segment of music in all twelve keys.

### Expression
The Expression tool is designed to offer you an imaginative headstart towards creating a piece of music. It offers the user random generations between one of six musical dynamics:
- Volume
- Tempo
- Articulation
- Mood
- Motion

Select the dynamic you want to explore in the tool's settings. The volume, tempo, and articulation dynamics have two versions: one with informal terms, and another with theoretical terminology. The mood and motion dynamics are intentionally abstract and vague, in order to offer the user a greater chance of musical exploration.
#### Example Uses
- Assist improvisation with a creative headstart.
- Explore music outside your normal styles of playing.

## Why should I use this?
Music and composition are creative arts. These tools were primarily created to assist the rote practice of instrumentation that can bolster musical skill. It may be challenging to track which things we have practiced and how we have done it, and at times we may find our music stuck inside patterns we've never known to explore beyond. These tools are designed to help a musician navigate these sorts of challenges.

## What is left?
The original Musical Practice Tools was written in JavaScript in the React framework, and had a metronome which worked together with the tools to generate changes at timed intervals. The rewrite of the application in ClojureScript changed the structure of the entire application. It's now more extensible and easier to modify.

There are a variety of features that could be (re/)implemented and built upon their original counterparts. I would really like to add:
- Recording functionality
- Tool Presets
- Metronome Volume Controls
- Select next keys using the circle of fifths and/or chromatically.

All among other changes!

## Feedback
Use the feedback link on the tool itself (click the sunshine!) to submit any feedback you may have, or open an issue here on GitHub. I hope these tools may serve you in some way.
