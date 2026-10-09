package com.amos.ams.web;

import com.amos.ams.agent.WorkflowAgentService;
import com.amos.ams.dto.Dtos;
import com.amos.ams.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final WorkflowAgentService agent;
    private final CurrentUser currentUser;

    public AgentController(WorkflowAgentService agent, CurrentUser currentUser) {
        this.agent = agent;
        this.currentUser = currentUser;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ACTIVITY_VIEW')")
    public Dtos.AgentStatus status() {
        return agent.status();
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAuthority('ACTIVITY_VIEW')")
    public Dtos.AgentReply ask(@Valid @RequestBody Dtos.AgentAsk request) {
        return agent.run(request, currentUser.require());
    }
}
