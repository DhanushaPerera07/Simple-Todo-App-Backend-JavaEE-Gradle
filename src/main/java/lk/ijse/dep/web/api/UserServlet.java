package lk.ijse.dep.web.api;


import lk.ijse.dep.web.model.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Dhanusha Perera
 * @since : 10/01/2021
 **/
@WebServlet(name = "UserServlet", urlPatterns = "/users")
public class UserServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Let's the id from the request header */
        int userIdInteger = 0;
        String userId = req.getParameter("id");

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");


        /* Validation */
/*        if (userId.isEmpty() || userId == ""){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);

            // Validation - to check the given id is an integer or not
            try{
                Integer.parseInt(userId);
            } catch (NumberFormatException exception){
                exception.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }*/

        PrintWriter out = resp.getWriter();
//            Class.forName(CommonConstant.MYSQL_DRIVER_CLASS_NAME);

        try (Connection connection = cp.getConnection()) {
            /* if user id is passed in the GET request that means,
             * record for that particular ID should be retrieved from the database, otherwise;
             * all the users in the database are retrieved */
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user`" +
                    ((userId != null) ? " WHERE `id`=?" : ""));
            if (userId != null) {
                pstm.setObject(1, userId);
            }
            ResultSet rst = pstm.executeQuery();

            /* Let's take the result set to a user array */
            List<User> userList = new ArrayList<>();

            /* Let's go through the result set */
            while (rst.next()) {
                int id = rst.getInt(1);
                String name = rst.getString(2);
                String passwordEncrypted = rst.getString(3);

                userList.add(new User(Integer.toString(id), name, passwordEncrypted));
            }

            /* If userId is not null, that means there is a user ID, somehow it is a valid one.
             * But, userList is empty; that means for that given ID no result found / no matching records found.
             * So, it is good to let the client know that there is no result for that request.
             * To do that, we can send "404 - Not Found" error */
            if (userId != null && userList.isEmpty()) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                /* Create Jsonb and serialize */
                Jsonb jsonb = JsonbBuilder.create();
                /* Let's make the userList to a JSON format
                 * and, send the JSON to the client */
                out.println(jsonb.toJson(userList));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }// doGet

    @Override /* WORKS FINE */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        /* Variable is used to store, when new user is created successfully, return its generated ID */
        int userGeneratedId = 0;

        /* Set Response Content Type */
        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        PrintWriter out = resp.getWriter();
//            Class.forName(CommonConstant.MYSQL_DRIVER_CLASS_NAME); // basicDataSource.setDriverClassName("mysql.driver_class");

        User user;
        try (Connection connection = bds.getConnection()) {
            if (req.getContentType().equals("application/json")) {
                /* application/x-www-form-urlencoded */
                Jsonb jsonb = JsonbBuilder.create();
                user = jsonb.fromJson(req.getReader(), User.class);
            } else if (req.getContentType().equals("application/x-www-form-urlencoded")) {
                /* application/x-www-form-urlencoded */
                user = new User("", // ID is auto generated
                        req.getParameter("name"),
                        req.getParameter("password")
                );

            } else {
                /* other ContentTypes are not acceptable */
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part - check null */
            if (user.getName() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part */
            if (user.getName().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }


            /* Check the user-given username is already taken or not */
            if (checkUserNameIsTaken(connection, user.getName())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO `user` (`name`) VALUES (?);",
                    Statement.RETURN_GENERATED_KEYS);
            pstm.setObject(1, user.getName());

            /* Check inserted successfully or not */
            if (pstm.executeUpdate() > 0) {
                try (ResultSet rs = pstm.getGeneratedKeys()) {
                    if (rs.next()) {
                        userGeneratedId = rs.getInt(1);
                    }
                }

                Jsonb jsonb = JsonbBuilder.create();
                out.println(jsonb.toJson(userGeneratedId));
                /* insertion successful */
                resp.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                /* insertion unsuccessful */
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (SQLIntegrityConstraintViolationException exception) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            exception.printStackTrace();
        } catch (SQLException exception) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            exception.printStackTrace();
        } catch (JsonbException exception) {
            exception.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    } //doPost

    @Override /* WORKING FINE */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");
        /* Set ContentType*/
//        response.setContentType("application/json");

        User user;
        try (Connection connection = basicDataSource.getConnection()) {
            /* Printer */
            PrintWriter out = response.getWriter();

            /* Map the JSON to a UserDTO java object */
            Jsonb jsonb = JsonbBuilder.create();
            user = jsonb.fromJson(request.getReader(), User.class);

            /* Validation */
            if (user.getName().isEmpty() || user.getPassword().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* TODO: password strength validation */

            /* Check the user-given username is in the database */
            if (!checkUserNameIsTaken(connection, user.getName())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            PreparedStatement pstm = connection.prepareStatement(
                    "UPDATE `user` SET `password`=? WHERE `name`=?;"
            );

            /* using hash function we can encrypt the password,
             * In order to do that we need to use commons-codec lib from apache
             * Classname is DigestUtils */
            String sha256HexPassword = DigestUtils.sha256Hex(user.getPassword());
            pstm.setObject(1, sha256HexPassword); // password
            pstm.setObject(2, user.getName().trim()); // username

            if (pstm.executeUpdate() > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (JsonbException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (SQLIntegrityConstraintViolationException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }// doPut

    @Override /* WORKING FINE */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        if (name == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");

            try (Connection connection = basicDataSource.getConnection()) {
                /* Check the user-given username is in the database */
                if (!checkUserNameIsTaken(connection, name)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `user` WHERE `name`=?;");
                preparedStatement.setString(1, name);

                if (preparedStatement.executeUpdate() > 0) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

    } // doDelete

    /**
     * This method will check given username is already in the database
     *
     * @return false : if given username is not taken
     * otherwise: username is already taken
     */
    public boolean checkUserNameIsTaken(Connection connection, String username) {

        try {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE `name`=?;");
            pstm.setObject(1, username);

            ResultSet resultSet = pstm.executeQuery();
            while (resultSet.next()) {
                return true;
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return false;
    }// doDelete

}
