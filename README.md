# What is RecipeVault
A piece of personal software that lets me keep track of all my recipes in one place.

To add a recipe, I only need to give it a name and write the method. Ingredients are tagged in the method using this syntax: @ingredient_name(quantity). 

With this, I can infer the ingredient list instead of typing them all out once for the ingredients list and again in the method.
To make this tagging process less laborious, I included some simple autocomplete functionality to suggest the names of existing ingredients as I type. Tapping a suggestion inserts it, and puts the cursor between the brackets ready for the quantity.

The biggest benefit is that the ingredients and their quantities are _actually in the method_, so I don't need to [mise en place](https://en.wikipedia.org/wiki/Mise_en_place) or scroll up and down between the method and ingredient sections to get the quantities.

I also sort the ingredient section based on order of use, useful if I know the method and only need the quantities - I just need to go down the list of ingredients.

As photography does not number among my skills, I decided to use AI to generate images of the final products. This saves me whole _minutes_ of getting lighting and angles just right, only to end up with a perfectly average picture of a loaf of bread.
Since I was already adding image generation, I thought I might as well do it for ingredients too.

## Tech
- Android with Jetpack Compose
- Dagger, Hilt for dependency injection
- Room for local storage
- OpenAI gpt-image-1

## Future Ideas
- [ ] Replce the tagging method: Use some kind of NER or just an LLM to pull ingredients and quantities out of plain text, rewrite steps to include quantities
- [ ] Allow import from web links: Maybe integrate with the android share feature to send links to recipes to the app.
