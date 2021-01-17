package lk.ijse.dep.web.api;

import lk.ijse.dep.web.model.Status;
import lk.ijse.dep.web.model.TodoItem;
import lk.ijse.dep.web.model.User;
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
 * @since : 16/01/2021
 **/
@WebServlet(name = "ItemServlet", urlPatterns = {"/todo"})
public class ItemServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Let's the id from the request header */
        String itemId = req.getParameter("id");
        String userId = req.getParameter("userId");

        System.out.println("--------------------------");
        System.out.println("Item ID: " + itemId);
        System.out.println("User ID: " + userId);

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        PrintWriter out = resp.getWriter();

        try (Connection connection = cp.getConnection()) {
            /* if user id is passed in the GET request that means,
             * record for that particular ID should be retrieved from the database, otherwise;
             * all the users in the database are retrieved */
            PreparedStatement pstm;
            if (itemId == null && userId != null) {
                pstm = connection.prepareStatement("SELECT t.id,t.content,t.date,t.status,t.user_id,u.name,u.password\n" +
                        "FROM `todo` t\n" +
                        "INNER JOIN `user` u ON t.user_id = u.id\n" +
                        "WHERE u.name=?");
                pstm.setString(1, userId);
            } else {

                pstm = connection.prepareStatement("SELECT t.id,t.content,t.date,t.status,t.user_id,u.name,u.password " +
                        "FROM `todo` t\n" +
                        "INNER JOIN `user` u ON t.user_id = u.id" +
                        ((itemId != null && userId != null) ? " WHERE u.name=? AND t.id=?" : ""));
                pstm.setObject(1, userId);
                try {
                    pstm.setObject(2, Integer.parseInt(itemId));
                } catch (SQLException exception) {
                    exception.printStackTrace();
                } catch (NumberFormatException exception) {
                    exception.printStackTrace();
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            }

//            if (itemId != null) {
//            }
            ResultSet rst = pstm.executeQuery();
            /* Let's take the result set to a user array */
            List<TodoItem> itemList = new ArrayList<>();
            /* Let's go through the result set */
            while (rst.next()) {
                itemList.add(new TodoItem(
                        Integer.toString(rst.getInt(1)), // todoItem id
                        rst.getString(2), // content
                        rst.getDate(3), // date
                        Status.valueOf(rst.getString(4)), // status
                        new User(Integer.toString(rst.getInt(5)), // user id
                                rst.getString(6), // user name
                                rst.getString(7)) // user password
                ));
            }
            /* If itemId is not null, that means there is a item ID, somehow it is a valid one.
             * But, itemList is empty; that means for that given ID no result found / no matching records found.
             * So, it is good to let the client know that there is no result for that request.
             * To do that, we can send "404 - Not Found" error */
            if (itemId != null && itemList.isEmpty()) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                /* Create Jsonb and serialize */
                Jsonb jsonb = JsonbBuilder.create();
                /* Let's make the userList to a JSON format
                 * and, send the JSON to the client */
                out.println(jsonb.toJson(itemList));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }// doGet

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Variable is used to store, when new user is created successfully, return its generated ID */
//        int userGeneratedId = 0;

        /* Set Response Content Type */
        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

//        PrintWriter out = resp.getWriter();

        TodoItem todoItem;
        try (Connection connection = bds.getConnection()) {
            if (req.getContentType().equals("application/json")) {
                /* application/x-www-form-urlencoded */
                Jsonb jsonb = JsonbBuilder.create();
                todoItem = jsonb.fromJson(req.getReader(), TodoItem.class);
            } else if (req.getContentType().equals("application/x-www-form-urlencoded")) {
                /* application/x-www-form-urlencoded */
                todoItem = new TodoItem("", // ID is auto generated
                        req.getParameter("content"),
                        new Date(System.currentTimeMillis()),
                        Status.valueOf(req.getParameter("status")),
                        new User(req.getParameter("user_id"), "", "")
                );

            } else {
                /* other ContentTypes are not acceptable */
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part - check null */
            if (todoItem.getContent() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part */
            if (todoItem.getContent().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }


            PreparedStatement pstm = connection.prepareStatement("INSERT INTO `todo` (`content`,`status`,`user_id`) VALUES (?,?,?);");
            pstm.setString(1, todoItem.getContent());
            pstm.setString(2, todoItem.getStatus().toString());
            pstm.setInt(3, Integer.parseInt(todoItem.getUser().getUserId()));

            /* Check inserted successfully or not */
            if (pstm.executeUpdate() > 0) {
//                Jsonb jsonb = JsonbBuilder.create();
//                out.println(jsonb.toJson(userGeneratedId));
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
    } // doPost


    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Set Response Content Type */
        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

//        PrintWriter out = resp.getWriter();

        TodoItem todoItem;
        try (Connection connection = bds.getConnection()) {
            if (req.getContentType().equals("application/json")) {
                /* application/x-www-form-urlencoded */
                Jsonb jsonb = JsonbBuilder.create();
                todoItem = jsonb.fromJson(req.getReader(), TodoItem.class);
            } else if (req.getContentType().equals("application/x-www-form-urlencoded")) {
                /* application/x-www-form-urlencoded */
                todoItem = new TodoItem("", // ID is auto generated
                        req.getParameter("content"),
                        new Date(System.currentTimeMillis()),
                        Status.valueOf(req.getParameter("status")),
                        new User(req.getParameter("user_id"), "", "")
                );

            } else {
                /* other ContentTypes are not acceptable */
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part - check null */
            if (todoItem.getContent() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part */
            if (todoItem.getContent().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }


            PreparedStatement pstm = connection.prepareStatement(
                    "UPDATE `todo` SET `content`=?,`status`=? WHERE `id`=? AND `user_id`=?;"
            );
            pstm.setString(1, todoItem.getContent());
            pstm.setString(2, todoItem.getStatus().toString());
            pstm.setInt(3, Integer.parseInt(todoItem.getTodoItemId()));
            pstm.setInt(4, Integer.parseInt(todoItem.getUser().getUserId()));

            /* Check inserted successfully or not */
            if (pstm.executeUpdate() > 0) {
//                Jsonb jsonb = JsonbBuilder.create();
//                out.println(jsonb.toJson(userGeneratedId));
                /* insertion successful */
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
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
    } // doDelete

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String itemId = req.getParameter("itemId");
        if (username == null || itemId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");

            try (Connection connection = basicDataSource.getConnection()) {
                /* Check the user-given username is in the database */
                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `todo` WHERE `id`=? AND `user_id`=(SELECT u.id\n" +
                        "FROM `user` u\n" +
                        "WHERE u.name=?);");
                try {
                    preparedStatement.setInt(1,Integer.parseInt(itemId));
                    preparedStatement.setString(2,username);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                } catch (NumberFormatException exception) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    exception.printStackTrace();
                }

                if (preparedStatement.executeUpdate()>0){
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }
}
