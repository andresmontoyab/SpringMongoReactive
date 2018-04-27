# Spring Reactive !!

Como se menciono antes dentro de SpringFramework existen diferentes formas de trabajar, una de ellas es la tradicional y otra es con el paradigma reactivo, para utilizar el paradigma reactivo en Spring es necesario utilizar ciertas clases que nos permitan el adecuado manejo de la informacion segun este paradigma, para esto en esta aplicacion mostraremos la transcion de una aplicacion tradicional a una aplicacion reactiva.

Adicionalmente esta aplicacion esta construida con una conexion a BD mongo.

## Spring Reactives Types

	1. Mono is a publiusher with zero or one elements in data stream.

	2. Flux is a publiser with zero or many elements in data stream.	

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


## Refactor Data Mongo Model

Debido a que Reactive Driver no soporta los DBRefs utilizados para nuestra BD mongo deberemos refactorizar el modelo de base de datos si queremos utilizar una aplicacion reactiva.


## Spring WebFlux 

The normal spring framework is blocking for this reason spring webflux was designed, spring staff created a new full stack inside of spring  called spring Webflux

	1. @Controller / RequestMapping --> Router Functions
	2. spring-webmvc 				--> spring-webflux
	3. Servlet APi 					--> HTTP / Reactive Program.
	4. Servlet container 			--> Tomcat, Jetty, Netty


Es importante decir que en un proyecto Spring no podran convivir Spring MVC y Spring WebFlux, por ende deberemos elegir cual utilizar, esto lo hacemos añadiendo o eliminando la dependencia en nuestro archivo gradel.

		1. Spring MVC: compile('org.springframework.boot:spring-boot-starter-web')

		2. Spring WebFlux : compile('org.springframework.boot:spring-boot-starter-webflux')

Si ejecutamos nuestro proyecto como WebFlux todo nuestro flujo estará orientado a hacer reactivo, por ende podremos hacer los siguiente refactos.

		1. En nuestros controladores cuando retornemos nuestros objetos a thymeleaf, podrmeos quitar la sentencia block(), y retornar el Mono<> o el Flux<> sin ningun problema, asi nuestro thymeleaf tambien trabajará sobre tipos reactivos.

		2. En algunas ocasiones se presentan fallos con los BindingResult por ende deberemos implementar la siguiente solucion.		


## Router Functions

Las router funciontions hace el simil de @Controller en WebFlux, a continuacion crearemos una.

			@Configuration
			public class routerFunction {

			    @Bean
			    RouterFunction<?> routes(RecipeService recipeService){
			        return RouterFunctions.route(GET("/api/recipes"),					// url donde será consumida
			                serverRequest -> ServerResponse								
			                        .ok()												// la respuesta del server sea ok
			                        .contentType(MediaType.APPLICATION_JSON)			// Retornará un tipo JSON
			                        .body(recipeService.getRecipes(), Recipe.class));	// Retornárá un objeto de la clase Recipe.class 

			    }
			} 


Adicionalmente a estas funciones tambien podremos hacerle sus pruebas unitarias.

			public class RouterFunctionTest {

			    WebTestClient webTestClient;

			    @Mock
			    RecipeService recipeService;

			    @Before
			    public void setUp() throws Exception {
			        MockitoAnnotations.initMocks(this);

			        routerFunction webConfig = new routerFunction();

			        RouterFunction<?> routerFunction = webConfig.routes(recipeService);

			        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
			    }


			    @Test
			    public void testGetRecipes() throws Exception {

			        when(recipeService.getRecipes()).thenReturn(Flux.just());

			        webTestClient.get().uri("/api/recipes")
			                .accept(MediaType.APPLICATION_JSON)
			                .exchange()
			                .expectStatus().isOk();
			    }

			    @Test
			    public void testGetRecipesWithData() throws Exception {

			        when(recipeService.getRecipes()).thenReturn(Flux.just(new Recipe(), new Recipe()));

			        webTestClient.get().uri("/api/recipes")
			                .accept(MediaType.APPLICATION_JSON)
			                .exchange()
			                .expectStatus().isOk()
			                .expectBodyList(Recipe.class);
			    }
			}			

## Manejo de Excepciones

Anteriormente con Spring MVC podiamos utilizar la clase ModelAndView para el manejo de excepciones, sin embargo esta es especifica para Spring MVC con Spring WebFlux no nos servirá por ende, deberemos refactorizar nuevametne nuestras excepciones quedando de la siguiente manera.

			@Slf4j
			@ControllerAdvice
			public class ControllerExceptionHandler {

			    @ResponseStatus(HttpStatus.BAD_REQUEST)
			    @ExceptionHandler(WebExchangeBindException.class)
			    public String handleNumberFormat(Exception exception, Model model){

			        log.error("Handling Binding Exception");
			        log.error(exception.getMessage());

			        model.addAttribute("exception", exception);
			        return "400error";
			    }
			}

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