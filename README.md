# Spring Reactive !!

Como se menciono antes dentro de SpringFramework existen diferentes formas de trabajar, una de ellas es la tradicional y otra es con el paradigma reactivo, para utilizar el paradigma reactivo en Spring es necesario utilizar ciertas clases que nos permitan el adecuado manejo de la informacion segun este paradigma, para esto en esta aplicacion mostraremos la transcion de una aplicacion tradicional a una aplicacion reactiva.

Adicionalmente esta aplicacion esta construida con una conexion a BD mongo.

## Repositorios Reactivos

Para poder utilizar los repositorios de Spring en el paradiga reactivo deberemos realizar una interface que exporte de la siguiente clase.

		1. ReactiveMongoRepository<Object , ID>

				public interface UnitOfMeasureReactiveRepository extends ReactiveMongoRepository<UnitOfMeasure, String> {
		}

		2. Adicionalmente si en esta interface se realizaron firmas de metodos se deberan cambiar para las clases publisher Mono o Flux.


				Mono<UnitOfMeasure> findByDescription(String description);
				Mono<Category> findByDescription(String description);

## Service Layer

En la implementacion tradicional nuestra capa de servicio, usualmente retornaba o un objeto en particular (objeto de nuestro dominio) o un Set o Lista de objetos de nuestro dominio, sin embargo para la programacion reactiva y con base en el metodo publisher suscriber deberemos utilizar las clases Mono o Flux, por ende deberemos refactorizar nuestra capa de servicio.

			1. Para empezar deberemos ir a las interfaces de nuestros servicios y adaptar las firmas de los metodos a Mono o Flux como se muestra a continuacion.

			Si estaba:

				ObjectName methodOne(String parameter1, String parameterTwo);

				Quedará 
				
				Mono<ObjectName> methodOne(String parameter1, String parameterTwo);

			Si era una coleccion.

				Set<ObjectName> methodOne(String parameter1, String parameterTwo);

				Quedará

				Flux<ObjectName> methodOne(String parameter1, String parameterTwo);

			2. Posteriormente deberemos cambiar la implementacion de estos metodos y asegurarnos que el objeto a retornar sea un publisher, adicionalmente en nuestras clases que implementan estas interfaces deberemos utilizar los repositoriosReactivos, a continuacion se mostrará un ejemplo de transformacion de la implementacion.


			Antes: 

			 Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);

		        if (!recipeOptional.isPresent()){
		            //todo impl error handling
		            log.error("recipe id not found. Id: " + recipeId);
		        }

		        Recipe recipe = recipeOptional.get();

		        Optional<IngredientCommand> ingredientCommandOptional = recipe.getIngredients().stream()
		                .filter(ingredient -> ingredient.getId().equals(ingredientId))
		                .map( ingredient -> ingredientToIngredientCommand.convert(ingredient)).findFirst();

		        if(!ingredientCommandOptional.isPresent()){
		            //todo impl error handling
		            log.error("Ingredient id not found: " + ingredientId);
		        }

		        //enhance command object with recipe id
		        IngredientCommand ingredientCommand = ingredientCommandOptional.get();
		        ingredientCommand.setRecipeId(recipe.getId());

		        return ingredientCommandOptional.get();


	        Despues(Reactivo):


		        return recipeRepository.findById(recipeId)									// Utiliza el repositorio para traer un recipe de tipo Mono<Recipe>
	                .map(recipe -> recipe.getIngredients()									// Transforma Mono<Recipe> a Set<Ingredients>
	                        .stream()														// Transforma Set<Ingredient> a Stream<Ingredients>
	                        .filter(ingredient -> ingredient.getId().equals(ingredientId))	// Filtra por Id del ingrediente
	                        .findFirst())													// Solo trae uno y transforma Stream<Ingrediente> a Optional<Ingredient>
	                .filter(Optional::isPresent)											// Valida que haya algo en el optional y retorna Mono<Ingredient>
	                    .map(ingredient -> {												// Transformo Mono<Ingrediente> a Mono<IngredienteCommand> 
	                    IngredientCommand command = ingredientToIngredientCommand.convert(ingredient.get());
	                    command.setRecipeId(recipeId);
	                    return command;
	                });

	        Refactor del modo Reactivo, adicionalmente podemos utilizar la clase Stream perteneciente a los publisher de la siguiente manera.

	                return   recipeReactiveRepository.findById(recipeId)	
		                .flatMapIterable(Recipe::getIngredients)						// con este metodo podremos iterar como si fuera un stream.
		                .filter(ingredient -> ingredient.getId().equals(ingredientId))
		                .single()
		                .map(ingredient -> {
		                    IngredientCommand command = ingredientToIngredientCommand.convert(ingredient.get());
		                    command.setRecipeId(recipeId);
		                    return command;
		                });




## Archivo Gradle


			1. Para nuestro ejemplo nuestro archivo gradle lucirá de la siguiente manera.


			buildscript {
				ext {
					springBootVersion = '2.0.0.RELEASE'
				}
				repositories {
					mavenCentral()
					maven { url "https://repo.spring.io/snapshot" }
					maven { url "https://repo.spring.io/milestone" }
				}
				dependencies {
					classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
				}
			}

			apply plugin: 'java'
			apply plugin: 'eclipse'
			apply plugin: 'org.springframework.boot'
			apply plugin: 'io.spring.dependency-management'
			apply plugin: 'jacoco'

			version = '0.0.1-SNAPSHOT'
			sourceCompatibility = 1.8

			repositories {
				mavenCentral()
				maven { url "https://repo.spring.io/snapshot" }
				maven { url "https://repo.spring.io/milestone" }
			}

			dependencies {
				compile('org.springframework.boot:spring-boot-starter-data-mongodb')
				compile('org.springframework.boot:spring-boot-starter-thymeleaf')
				compile('org.springframework.boot:spring-boot-starter-web')
				compile('org.springframework.boot:spring-boot-starter-webflux')
				runtime('org.springframework.boot:spring-boot-devtools')
				compile('de.flapdoodle.embed:de.flapdoodle.embed.mongo')
				compile('org.springframework.boot:spring-boot-starter-data-mongodb-reactive')
				compile group: 'cz.jirutka.spring', name: 'embedmongo-spring', version: '1.3.1'
				compile 'org.webjars:bootstrap:3.3.7-1'
				compileOnly('org.projectlombok:lombok')
				testCompile('org.springframework.boot:spring-boot-starter-test')
				testCompile('io.projectreactor:reactor-test')

			}
			//export test coverage
			jacocoTestReport {
			    reports {
			        xml.enabled true
			        html.enabled false
			    }
			}


			3. Con esto será suficiente para poder instanciar nuestro nuevo repositorio reactivo.