# Creator's Kit
Creator's Kit provides tools for creators of all sorts to make in-game scenes by spawning and merging models from the cache to animate and program.

![Title](https://imgur.com/ngUlJdQ.gif)

A video guide on how to use the plugin can be found through [this link](https://www.youtube.com/watch?v=E_9c-LwDRRY&ab_channel=ScreteMonge).

## Creator's Panel

The kit functions around the Creator's panel, where  you have several options to open new menus, as well as the ability to add and modify objects.

![Kit](https://imgur.com/UxGhXNs.png)

### Objects

The Add Object button adds a new object to your panel. From here, you can:
- Rename your object (for organizational purposes)
- Set its model ID (to take the model from the cache and use it)
- Set its radius (for larger models, increase the radius until the object stops clipping with the surrounding tiles)
- Set its animation ID

The buttons on the left side:
- Swap between ID and Custom models. ID will allow you to grab model IDs from the cache as described above, while Custom mode uses models you take from the environment or Model Anvil
- Spawn or despawn the object
- Relocate the object to your location
- Set whether the animation is active or not

The buttons on top, from left to right:
- Duplicate
- Minimize/Maximize
- Delete

Model IDs and Animation IDs can be found through various sources across the internet. Two of them I frequently use are [RuneMonk](https://runemonk.com/tools/entityviewer/) and the [Wiki Cache Dump](https://chisel.weirdgloop.org/moid/index.html).

### Config - Scene

![Scene](https://imgur.com/5QhpyDm.png)

In the config, you'll find a few different options for manipulating the object in the scene:
- Quick Spawn toggles the spawn/despawn status of the Selected object
- Quick Location places the Selected object at your cursor's hovered tile
- Quick Rotate rotates the Selected object clockwise or counter-clockwise

### Config - Overlays

![Overlays](https://imgur.com/6wd3NeX.png)

To help with getting models, animations, and generally organizing your scene, there's also a Toggle Overlays button that effectively gives a Debug mode to help study the environment.

You can select which overlays to have active at any given time. Two important ones for this plugin are My Object and Object Path.

![MyObject](https://imgur.com/Zzw355q.png)

## Object Manager

You can grab the models of objects, NPCs, and players from the environment by right-clicking them and selecting "Store"

![Store](https://imgur.com/SJN4l4u.png)
![Jail](https://imgur.com/6JZb62X.png)

These will be added as options for a Custom model as described above

![CustomModel](https://imgur.com/SFcQvQa.png)

You can manage the options in this menu by clicking the Organizer button at the top of the panel. From here, you can rename or remove unneeded custom models as necessary.

![Organizer](https://imgur.com/EqcAP1r.png)

## Anvil

The Anvil is a tool for modifying and merging models from the game cache to use as Custom models for your objects. It can be found by clicking the Anvil button at the top of the panel.

![Anvil](https://imgur.com/nUSvIml.png)

The Anvil works by allowing you to add models from the cache to the palette, from where you can change many of its parameters - like rotating, scaling, or translating it. You can add multiple models to merge them together into a single object, which will allow you to animate and program them together.

### Setup
- Name: Sets the name by which the new Custom model can be found in the Custom model menus
- Lighting: Default lighting is moderate, Actor Lighting works well for Players and NPCs, and No Lighting provides no light
- Forge: Takes all the models from the palette and turns them into a Custom model
- Forge & Set: Same as above and also sets the Selected object to the new Custom model

### Palette Organization
- Add: Adds a new model to the palette
- Clear: Removes all models from the palette
- Save: Takes all the models on the palette and saves their data together in a txt file
- Load: Load a previously saved model to add to the palette
- Priority: Can help when merging some models to prevent clipping

### Model Modification

![Palette](https://imgur.com/ctGkaBL.png)

Each model on the palette can be individually modified:
- Name & Arrows: Organizational features for your convenience
- Model ID: Determines the model to modify from the cache
- Group #: Sets the Group number for Group Transformations (later)
- Duplicate/Delete: Duplicates or Deletes the given model
- Transform: Transform the model either by 1 full tile or 1/128th of a tile in the x/y/z direction (positive is East/North/Up, negative is West/South/Down)
- Scale: Scales the model in the x/y/z direction
- Rotate: Rotates the model by 90, 180, or 270 degrees

![Colours](https://imgur.com/LPJxhWV.png)

Runescape models often take previously used assets and recolour them. The colour options presented here let you do the same. The Colour Swapper opens a menu for such modifications, but a few features like copying, pasting, and clearing the modified colours exist in the palette for convenience.

![Colours2](https://imgur.com/pKsFszk.png)

Old Colours are the default colours of the model, while New Colours are those you wish to replace the Old Colours. In this case, the grey of the helmet is replaced with a lime green.

![Colours3](https://imgur.com/Sdxdtik.png)
![Colours4](https://imgur.com/g7dIQUj.png)

## Programming

This plugin also features a programming function to set your objects on a path to walk.

### Setting a Path

![Path](https://imgur.com/6yE9GKy.gif)

You can set a path using the hotkeys described in the config menu.

![ConfigPath](https://imgur.com/d9AjHYo.png)

- Add Program Step: Adds a step for the object to navigate to at the hovered tile for the Selected object
- Remove Program Step: Removes the step from the hovered tile for the Selected object
- Clear Program Steps: Removes all steps from the Selected object's path
- Play/Pause Toggle: Toggles the Play status of the Selected object
- Play/Pause All: Toggles the Play status of all objects
- Reset Locations: Reset the Selected object's location to its first step
- Reset All Locations: Resets all objects' locations to their first step

### Modifying the Program

You can further modify each object's program in the Programmer at the top of the panel.

![Programming](https://imgur.com/meXOAzA.png)

- Idle Animation: Sets the animation for when the program is inactive or finished
- Active Animation: Sets the animation for when the object is actively moving
- Speed: Sets the object's speed
- Turn Speed: Sets how fast the object rotates at turns
- Movement Type: Sets where the object is allowed to travel. Normal is regular movement on land, Waterborne will restrict the object to water, and Ghost will ignore all movement restrictions

## Camera Options

There's also a few camera features like Oculus Orb mode which can be found in the config.

![Camera](https://imgur.com/Bob7jx4.png)

- Toggle Oculus Orb Mode: Hotkey to jump into and out of Oculus Orb mode
- Orb Speed: Determines the orb's speed
- AutoRotate options: Allows you to automatically set a camera rotation for certain rotating shots
- Rotation Speed: Sets the speed of AutoRotation

## Credits
- Special thanks to Craig Wood for the [JTree Drag and Drop functionality](https://coderanch.com/t/346509/java/JTree-drag-drop-tree-Java) as well as Albert Hendriks for the [improved version.](https://gitlab.com/alberthendriks/jtree-drag-drop)
- Special thanks to Yona-Appletree for the [HSLColor functions](https://gist.github.com/Yona-Appletree/0c4b58763f070ae8cdff7db583c82563)











