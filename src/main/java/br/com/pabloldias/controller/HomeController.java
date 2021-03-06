package br.com.pabloldias.controller;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import br.com.pabloldias.playlist.PlaylistInfo;
import br.com.pabloldias.service.SpotifyService;

@Controller
public class HomeController {
	
	@Autowired
	SpotifyService spotifyService;
	
    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }
    
    @RequestMapping(value = "/connect")
    public RedirectView connect(HttpServletRequest request) {
    	String authorizeURL = spotifyService.getAuthorizeURL();
    	RedirectView redirectView = new RedirectView();
        redirectView.setUrl(authorizeURL);
        return redirectView;
    }
    
    @GetMapping("/playlist")
    public String playlist(@RequestParam(value="code", required=false) String code, Model model) {
    	model.addAttribute("playlists", spotifyService.getPlaylists());
    	model.addAttribute("playlistInfo", new PlaylistInfo(code));
    	return "playlist";
    }
    
    @PostMapping("/playlist")
    public String createPlaylist(@ModelAttribute PlaylistInfo playlistInfo, Model model) {
    	System.out.println(playlistInfo);
    	spotifyService.createNewPlaylist(playlistInfo);
    	return "playlist";
    }
    
}
