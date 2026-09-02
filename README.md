# MVVM Movie
 A TMDB client built on MVVM.

 **Stack:** Kotlin + View Binding + MVVM + Hilt + Coroutines + Retrofit/OkHttp + Room + Glide

 **Features**
 - Popular / Now playing / Top rated / Upcoming lists, with endless scrolling
 - Movie search
 - Detail screen with genres, runtime, tagline, budget and revenue, cast, trailers and similar titles
 - Favourites, stored locally in Room
 - Client-side sorting of the loaded list by rating, release date or title
 - Offline fallback: the last loaded first page is cached and shown when the network is unreachable
 - Loading / empty / error states with retry, plus pull to refresh

 **Setup:** add your TMDB v3 key to `local.properties`:

 ```properties
 TMDB_API_KEY=your_key_here
 ```


https://user-images.githubusercontent.com/8773222/230867850-08d92276-fa11-4ac1-9056-1a44d62cf6aa.mp4





<img width="771" alt="Screenshot 2023-04-12 at 3 50 24 PM" src="https://user-images.githubusercontent.com/8773222/231389549-6ef280e0-6139-4c26-a553-d5817deaf9b1.png">
