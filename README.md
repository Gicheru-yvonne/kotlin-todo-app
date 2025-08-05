Kotlin Todo App

A mobile application built using Kotlin and Jetpack Compose that helps users manage their daily tasks across multiple todo lists. Designed for simplicity and productivity, this app supports task creation, completion tracking, due dates, and list-level insights, all powered by an embedded SQLite database.

 ✨ Features

- Multiple Todo Lists 
  Create and manage multiple task lists with unique names.

- Task Management  
  Add, edit, delete, and mark tasks as complete within any list.

- Due Dates   
  Tasks can have due dates, which help prioritize daily goals.

- Visual Feedback 
  -  Lists with overdue tasks
  -  Lists with tasks due today
  -  Live counters: total vs completed tasks

- Move Tasks Between Lists  
  Easily reassign tasks from one list to another.

- Local Persistence  
  Uses SQLite to store all lists and tasks offline.

- Jetpack Compose UI
  Built entirely with modern declarative UI — no XML layouts.

Tech Stack
 Layer- Technology          
 Language- Kotlin              
 UI-Jetpack Compose     
Storage- SQLite (via helper)  
Architecture- MVVM-inspired       
Tools- Android Studio, Git 



Project Structure
Kotlin Todo App/
├── app/
│ ├── src/
│ │ ├── main/java/...
│ │ ├── main/res/
│ ├── build.gradle.kts
├── .idea/
├── .gitignore
├── build.gradle.kts
└── settings.gradle.kts



