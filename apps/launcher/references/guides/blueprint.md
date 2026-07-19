# Pages

The app will require 2 different pages. 

- Tiles in start menu: 
    - Tiles are arranged in a grid of 4 columns by n rows. Each tile can be either 1x1, 2x2, 4x2. 
    - At the bottom of this page after the tile grid, there should be a right arrow pointing to the right, aligned right. Tapping on it would bring up the app list from the right. 
    - holding down on a tile would trigger the reorder menu where all tiles would be floating freely, have a darkening filter applied on it, all sans the tile being reordered on, which would be focused and stationary. At 2 corners of the active tile, there would be a resize button and an unpin button. One would change size 1x1 -> 2x2 -> 4x2 -> 1x1, the latter would remove tile from the grid. 
    - This page does not have a background and uses black as bg
    - Grid items have an inner spacing
- App Menu: 
    - All apps are to be listed in a list here.
    - Apps are grouped under lowercase letter markers (`#` for non-letter starters). Tapping a letter marker opens the standard find-by-letter overlay (`MetroJumpList`): 4-column grid of `#` + a–z + globe; accent tiles are active, gray tiles inactive. Tapping an active letter scrolls the list to that group.
    - Search: tapping the magnifying-glass button reveals a white search field with an accent border above the list and shows the keyboard (`images/applist_search_dark_blue.png`). While search is active, letter markers are hidden (`MetroJumpListLogic.showSectionMarkers`) and the list is a flat filtered result; the first case-insensitive match of the query in each app label is highlighted in accent. Tapping search again re-focuses the field and shows the keyboard. Dismiss search by closing the keyboard, pressing Back, or tapping the empty strip under the icon.