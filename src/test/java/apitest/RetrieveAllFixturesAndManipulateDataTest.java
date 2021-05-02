package apitest;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.testng.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.testng.Assert.*;

public class RetrieveAllFixturesAndManipulateDataTest {

    public static final int INITIAL_SIZE = 3;//Initialise to size
    private static final int TIMEOUT = 10;

    @BeforeClass
    public void before() {
        baseURI = "http://localhost:3000";
        dataReset();

    }

    /*
    Code below will delete newly created fixtures.
    Also helps to maintain current fixture and it will delete any additional fixture
     */

    @AfterTest
    public void after() {
        dataReset();
    }

    private void dataReset() {
        Response response = given().accept(ContentType.JSON)
                .when().get("/fixtures");
        List<Map<String, Object>> retrieveFixtures = response.body().as(List.class);
        for (int i = INITIAL_SIZE + 1; i < retrieveFixtures.size() + 1; i++) {
            given().contentType(ContentType.JSON)
                    .and().pathParam("id", i)
                    .when().delete("/fixture/{id}");
        }
    }

    /*
     * Create and delete a new fixture.
     * Assert that the fixture no longer exists
     */

    @Test(priority = 1)
    public void createNewFixture_And_DeletingNewFixture() throws IOException {
        int id = getLastId() + 1;
        String newFixtures = messageGenerator(id, "fixtureRequest.json");
        ValidatableResponse response = given().contentType(ContentType.JSON)
                .and().body(newFixtures)
                .when().post("/fixture").then().assertThat().statusCode(202);

        Response newFixtureResponse = getFixtureResponse(id);

        assertEquals(newFixtureResponse.statusCode(), 200);

        ValidatableResponse deleteResponse = given().contentType(ContentType.JSON)
                .and().pathParam("id", id)
                .when().delete("/fixture/{id}").then()
                .assertThat().statusCode(204);

        Response deletedFixtureResponse = given().accept(ContentType.JSON)
                .and().pathParam("id", id)
                .when().get("/fixture/{id}");

        //verify status code
        assertEquals(deletedFixtureResponse.statusCode(), 404);

    }

    /*
    Test below will retrieve all fixtures and will confirm all fixtures has fixtureId value.
    This is included necessary Assertion
     */

    @Test(priority = 2)
    public void retrieveFixtures_ConfirmingFixtureContainsId() {
        Response response = given().accept(ContentType.JSON)
                .when().get("/fixtures");
        assertEquals(response.statusCode(), 200);

        List<Map<String, Object>> listOfFixtures = response.body().as(List.class);

        assertEquals(listOfFixtures.size(), INITIAL_SIZE);

        for (int i = 0; i < listOfFixtures.size(); i++) {
            assertTrue(listOfFixtures.get(i).containsKey("fixtureId"));
        }

    }

    /**
     * Using post method to create new  fixture in fixtures
     * Using get method to confirm newly created fixture
     * Assert, that the first object has a teamId of 'HOME'.
     */
    @Test(priority = 3)
    public void createNewFixture_RetrieveNewFixture_AndConfirmingId() throws IOException {
        int id = getLastId() + 1;
        String createNewFixture = messageGenerator(id, "fixtureRequest.json");

        ValidatableResponse response = given().contentType(ContentType.JSON)
                .and().body(createNewFixture)
                .when().post("/fixture").then().assertThat().statusCode(202);

        Response newFixtureResponse = getFixtureResponse(id);

        assertEquals(newFixtureResponse.statusCode(), 200);

        //assign response to jsonpath
        JsonPath json = newFixtureResponse.jsonPath();

        String teamId = json.getString("footballFullState.teams[0].teamId");
        assertEquals(teamId, "HOME");

    }

    private Response getFixtureResponse(int id) {
        Response newFixtureResponse = null;
        for (int i = 0; i < TIMEOUT; i++) {
            newFixtureResponse = given().accept(ContentType.JSON)
                    .and().pathParam("id", id)
                    .when().get("/fixture/{id}");

            if (newFixtureResponse.statusCode() == 200) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return newFixtureResponse;

    }

    @Test(priority = 4)
    public void updatingExistingFixture() throws IOException {

        String updatedFixture = messageGenerator(1, "fixtureRequestForUpdate.json");

        ValidatableResponse response = given().contentType(ContentType.JSON)
                .and().body(updatedFixture)
                .when().put("/fixture").then().assertThat().statusCode(204);

        Response updatedFixtureResponse = given().accept(ContentType.JSON)
                .and().pathParam("id", "1")
                .when().get("/fixture/{id}");

        //assigning response to jsonpath
        JsonPath json = updatedFixtureResponse.jsonPath();

        assertEquals(json.getString("footballFullState.homeTeam"), "Besiktas");

        assertEquals(json.getString("footballFullState.awayTeam"), "Fenerbahce");

        assertEquals(json.getString("footballFullState.period"), "SECOND_HALF");

    }

    /*
     *
     *return find last fixture and return its fixture Id
     */

    private int getLastId() {
        Response response = given().accept(ContentType.JSON)
                .when().get("/fixtures");
        List<Map<String, Object>> allFixtures = response.body().as(List.class);
        return allFixtures.size();
    }

    /*
     *Generate Json Message with Id
     */

    private String messageGenerator(int id, String filename) throws IOException {
        String fileString = new String(Files.readAllBytes(Paths.get(filename)));
        return fileString.replace("ID_TO_BE_REPLACED", String.valueOf(id));

    }

}
