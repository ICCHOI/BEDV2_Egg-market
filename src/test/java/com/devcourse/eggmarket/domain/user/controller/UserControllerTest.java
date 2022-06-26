package com.devcourse.eggmarket.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.devcourse.eggmarket.domain.user.dto.UserRequest;
import com.devcourse.eggmarket.domain.user.dto.UserRequest.Save;
import com.devcourse.eggmarket.domain.user.dto.UserRequest.Update;
import com.devcourse.eggmarket.domain.user.dto.UserResponse;
import com.devcourse.eggmarket.domain.user.dto.UserResponse.FindNickName;
import com.devcourse.eggmarket.domain.user.dto.UserResponse.MannerTemperature;
import com.devcourse.eggmarket.domain.user.model.User;
import com.devcourse.eggmarket.domain.user.repository.UserRepository;
import com.devcourse.eggmarket.domain.user.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    User user;
    UserResponse.Basic userResponse;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();

        user = User.builder()
            .nickName("test1")
            .phoneNumber("01012345678")
            .password("Password!1")
            .role("USER")
            .build();

        UserRequest.Save saveRequest = new Save(user.getPhoneNumber(), user.getNickName(),
            user.getPassword(), false, null);

        userResponse = userService.save(saveRequest);
    }

    @AfterEach
    void clearUp() {
        userRepository.deleteAll();
    }

//    @Test
//    void signUp() throws Exception {
//        //Given
//        User newUser = User.builder()
//            .nickName("test2")
//            .phoneNumber("01011111111")
//            .password("Password!2")
//            .role("USER")
//            .build();
//
//        UserResponse expectResponse = UserResponse.builder()
//            .nickName(newUser.getNickName())
//            .mannerTemperature(36.5F)
//            .role(newUser.getRole().toString())
//            .build();
//
//        UserRequest.Save saveRequest = new UserRequest.Save(newUser.getPhoneNumber(),
//            newUser.getNickName(), newUser.getPassword(), false, null);
//
//        //when
//        MvcResult result = mockMvc.perform(post("/signup")
//            .content(objectMapper.writeValueAsString(saveRequest)))
//            .andReturn();
//
//        UserResponse saveResult = objectMapper
//            .readValue(result.getResponse().getContentAsString(), getUserResponseTypeReference());
//
//        //Then
//        assertThat(saveResult).usingRecursiveComparison().ignoringFields("id")
//            .isEqualTo(expectResponse);
//
//    }

    @Test
    void login() throws Exception {
        //Given
        UserResponse.Basic expectResponse = UserResponse.Basic.builder()
            .nickName(user.getNickName())
            .mannerTemperature(36.5F)
            .role(user.getRole().toString())
            .build();

        UserRequest.Login loginRequest = new UserRequest.Login(user.getNickName(),
            user.getPassword());

        //When
        MvcResult result = mockMvc.perform(post("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResult = result.getResponse().getContentAsString();

        //Then
        assertThat(loginResult).isEqualTo("true");
    }

    @Test
    void signOut() throws Exception {
        //Given
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserDetails userDetails = userService.loadUserByUsername(user.getNickName());
        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext);

        //When
        MvcResult result = mockMvc.perform(delete("/signout")
            .session(session))
            .andExpect(status().isOk())
            .andReturn();

        String signOutResult = result.getResponse().getContentAsString();

        //Then
        assertThat(signOutResult).isEqualTo(userResponse.id().toString());
    }

    @Test
    void getUserName() throws Exception {
        //Given
        UserResponse.FindNickName expectResult = UserResponse.FindNickName.builder()
            .nickName(user.getNickName()).build();

        //When
        MvcResult result = mockMvc.perform(get("/users/nickName")
            .param("phoneNumber", user.getPhoneNumber()))
            .andExpect(status().isOk())
            .andReturn();

        UserResponse.FindNickName findResult = objectMapper
            .readValue(result.getResponse().getContentAsString(), getFindNickNameTypeReference());

        //Then
        assertThat(findResult).usingRecursiveComparison().isEqualTo(expectResult);
    }

    @Test
    void changePassword() throws Exception {
        //Given
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserDetails userDetails = userService.loadUserByUsername(user.getNickName());
        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext);

        UserRequest.ChangePassword userRequest = UserRequest.ChangePassword.builder()
            .newPassword("NewPass!1").build();

        //When
        MvcResult result = mockMvc.perform(patch("/users/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String updateResult = result.getResponse().getContentAsString();

        //Then
        assertThat(updateResult).isEqualTo("true");
    }

    @Test
    void update() throws Exception {
        //Given
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserDetails userDetails = userService.loadUserByUsername(user.getNickName());
        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext);

        UserRequest.Update userRequest = new Update("01011111111", "updateNick", null);
        UserResponse.Update expectResponse = new UserResponse.Update(null,
            userRequest.phoneNumber(), userRequest.nickName(), user.getImagePath());

        //When
        MvcResult result = mockMvc.perform(put("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userRequest)))
            .andExpect(status().isOk())
            .andReturn();

        UserResponse.Update updateResult = objectMapper
            .readValue(result.getResponse().getContentAsString(), getUpdateTypeReference());

        //Then
        assertThat(updateResult).usingRecursiveComparison().ignoringFields("id")
            .isEqualTo(expectResponse);

    }

    @Test
    void getMannerTemperature() throws Exception {
        //Given
        UserResponse.MannerTemperature expectResponse = UserResponse.MannerTemperature.builder()
            .id(1L)
            .nickName(user.getNickName())
            .mannerTemperature(user.getMannerTemperature())
            .build();

        //When
        MvcResult result = mockMvc.perform(get("/users/mannerTemperature")
            .param("id", "1"))
            .andExpect(status().isOk())
            .andReturn();

        UserResponse.MannerTemperature mannerTempResult = objectMapper
            .readValue(result.getResponse().getContentAsString(), getMannerTempTypeReference());

        //Then
        assertThat(mannerTempResult).usingRecursiveComparison().isEqualTo(expectResponse);
    }

    private TypeReference<UserResponse.FindNickName> getFindNickNameTypeReference() {
        return new TypeReference<FindNickName>() {
            @Override
            public Type getType() {
                return super.getType();
            }

            @Override
            public int compareTo(TypeReference<FindNickName> o) {
                return super.compareTo(o);
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            @Override
            public String toString() {
                return super.toString();
            }
        };
    }

    private TypeReference<UserResponse.Update> getUpdateTypeReference() {
        return new TypeReference<UserResponse.Update>() {
            @Override
            public Type getType() {
                return super.getType();
            }

            @Override
            public int compareTo(TypeReference<UserResponse.Update> o) {
                return super.compareTo(o);
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            @Override
            public String toString() {
                return super.toString();
            }
        };
    }

    public TypeReference<UserResponse.MannerTemperature> getMannerTempTypeReference() {
        return new TypeReference<MannerTemperature>() {
            @Override
            public Type getType() {
                return super.getType();
            }

            @Override
            public int compareTo(TypeReference<MannerTemperature> o) {
                return super.compareTo(o);
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            @Override
            public String toString() {
                return super.toString();
            }
        };
    }
}