package com.example.tripbridgeserver.controller;

import com.example.tripbridgeserver.dto.RouteDTO;
import com.example.tripbridgeserver.entity.Route;
import com.example.tripbridgeserver.entity.UserEntity;
import com.example.tripbridgeserver.repository.RouteRepository;
import com.example.tripbridgeserver.repository.UserRepository;
import com.example.tripbridgeserver.service.RouteService;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouteController {
    private final RouteService routeService;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;

    @Autowired
    public RouteController(RouteService routeService, RouteRepository routeRepository, UserRepository userRepository){
        this.routeService = routeService;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/route")
    public Route enroll(@RequestBody RouteDTO dto){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        UserEntity currentUser = userRepository.findByEmail(userEmail);

        Route route = routeService.toEntity(dto,currentUser);
        routeService.calculateRouteOrder();
        return routeRepository.save(route);
    }

    @DeleteMapping("/route")
    public void deleteAllRoutes(){
        routeRepository.deleteAll();

    }

    @PostMapping("/route/update")
    public void updateRoutes() {
        routeService.calculateRouteOrder();
    }

}
