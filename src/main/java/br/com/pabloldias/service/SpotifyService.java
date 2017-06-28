package br.com.pabloldias.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.wrapper.spotify.Api;
import com.wrapper.spotify.HttpManager;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.methods.AddTrackToPlaylistRequest;
import com.wrapper.spotify.methods.PlaylistCreationRequest;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.methods.UserPlaylistsRequest;
import com.wrapper.spotify.methods.authentication.ClientCredentialsGrantRequest;
import com.wrapper.spotify.models.AuthorizationCodeCredentials;
import com.wrapper.spotify.models.ClientCredentials;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.Playlist;
import com.wrapper.spotify.models.PlaylistTrack;
import com.wrapper.spotify.models.SimplePlaylist;

import br.com.pabloldias.AppProperties;
import br.com.pabloldias.playlist.PlaylistInfo;

@Service
public class SpotifyService {

	@Autowired
	private AppProperties properties;

	@Autowired
	private Api api;

	@Bean
	public Api buildApi() {
		return Api.builder().clientId(properties.getClientId()).clientSecret(properties.getClientSecret())
				.redirectURI(properties.getRedirectURI()).build();
	}

	public String getAccessToken(Api api) {
		HttpManager httpManager = SpotifyHttpManager.builder().build();

		final ClientCredentialsGrantRequest clientCredentialsGrantRequest = api.clientCredentialsGrant()
				.httpManager(httpManager).build();

		ClientCredentials clientCredentials;
		try {
			clientCredentials = clientCredentialsGrantRequest.get();
			String accessToken = clientCredentials.getAccessToken();
			api.setAccessToken(accessToken);
			return accessToken;
		} catch (IOException | WebApiException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getAuthorizeURL() {
		try {

			final List<String> scopes = Arrays.asList("playlist-modify-public", "playlist-modify-private",
					"user-read-private", "user-read-email", "user-library-modify", "playlist-read-private");

			String authorizeURL = api.createAuthorizeURL(scopes, properties.getState());
			System.out.println(authorizeURL);
			return authorizeURL;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public List<SimplePlaylist> getPlaylists() {
		UserPlaylistsRequest.Builder playlistBuilder = api.getPlaylistsForUser(properties.getUserId());
		UserPlaylistsRequest userPlaylistsRequest = playlistBuilder.accessToken(getAccessToken(api)).build();
		Page<SimplePlaylist> playlistsPage = new Page<>();
		try {
			playlistsPage = userPlaylistsRequest.get();
			return playlistsPage.getItems();
		} catch (IOException | WebApiException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public void createNewPlaylist(PlaylistInfo playlistInfo) {
		authorize(playlistInfo.getAuthenticationCode());
		Optional<Playlist> newPlaylist = createPlaylist(playlistInfo);
		if (newPlaylist.isPresent()) {
			addTracks(newPlaylist.get(), getPlaylistTracks(playlistInfo));
		}

	}

	private void addTracks(Playlist playlist, List<PlaylistTrack> playlistTracks) {
		List<String> tracksToAdd = new ArrayList<>(playlistTracks.size());
		for (PlaylistTrack playlistTrack : playlistTracks) {
			tracksToAdd.add(playlistTrack.getTrack().getUri());
		}
		
		Integer pageSize = 100;
		Integer offset = 0;
		AddTrackToPlaylistRequest request;
		Boolean hasPages = true;
				
		while (hasPages) {
			request = api
					.addTracksToPlaylist(properties.getUserId(), playlist.getId(), tracksToAdd.subList(offset, offset + pageSize))
					.build();
			try {
				request.get();
				offset += pageSize;
				if (offset > tracksToAdd.size()) {
					hasPages = false;					
				}
			} catch (IOException | WebApiException e) {
				e.printStackTrace();
			}
		}

	}

	private List<PlaylistTrack> getPlaylistTracks(PlaylistInfo playlistInfo) {
		Integer pageSize = 100;
		Integer offset = 0;
		PlaylistTracksRequest request;
		List<PlaylistTrack> tracks = new ArrayList<>();
		Page<PlaylistTrack> page;
		Boolean hasPages = true;
				
		while (hasPages) {
			request = api
					.getPlaylistTracks(properties.getUserId(), playlistInfo.getOriginalPlaylist())
					.limit(pageSize)
					.offset(offset)
					.build();
			try {
				page = request.get();
				tracks.addAll(page.getItems());
				offset += pageSize;
				if (page.getNext() == null) {
					hasPages = false;					
				}
				System.out.println(page.getNext());
			} catch (IOException | WebApiException e) {
				e.printStackTrace();
			}
		}
		
		return tracks;
	}

	private Optional<Playlist> createPlaylist(PlaylistInfo playlistInfo) {
		PlaylistCreationRequest playlistCreationRequest = api
				.createPlaylist(properties.getUserId(), playlistInfo.getName()).publicAccess(true).build();
		try {
			return Optional.of(playlistCreationRequest.get());
		} catch (IOException | WebApiException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	private void authorize(String authenticationCode) {
		AuthorizationCodeCredentials creds;
		try {
			creds = api.authorizationCodeGrant(authenticationCode).build().get();
			api.setAccessToken(creds.getAccessToken());
			api.setRefreshToken(creds.getRefreshToken());
		} catch (IOException | WebApiException e) {
			e.printStackTrace();
		}
	}

}
