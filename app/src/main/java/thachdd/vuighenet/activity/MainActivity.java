package thachdd.vuighenet.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import thachdd.vuighenet.R;
import thachdd.vuighenet.adapter.MainRecyclerAdapter;
import thachdd.vuighenet.api_client.ApiClient;
import thachdd.vuighenet.api_client.ApiInterface;
import thachdd.vuighenet.api_client.EpisodesCallback;
import thachdd.vuighenet.api_client.SeasonsCallback;
import thachdd.vuighenet.listener.RecyclerItemClickedListener;
import thachdd.vuighenet.model.EpisodeDetail;
import thachdd.vuighenet.model.EpisodesResponse;
import thachdd.vuighenet.model.SeasonDetail;
import thachdd.vuighenet.model.SeasonsResponse;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar = null;
    private RecyclerView mRecycler = null;
    private AVLoadingIndicatorView mLoading = null;
    private RelativeLayout mLoadingContainer = null;
    private Spinner mSpinner = null;

    private SeasonsCallback mSeasonsCallback = null;
    private List<SeasonDetail> mSeasons = new ArrayList<>();
    private EpisodesCallback mEpisodesCallback = null;
    private List<EpisodeDetail> mEpisodes = new ArrayList<>();

    private MainRecyclerAdapter mAdapter = null;

    private final String MY_SP = "mysharedpreferences";
    private final String SEASON_POS ="seasonidx";
    private final String EPISODE_ID = "episodeid";
    private final String EPISODE_POS = "episodepos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        mRecycler = (RecyclerView) findViewById(R.id.main_recycler);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new MainRecyclerAdapter(this);
        mRecycler.setAdapter(mAdapter);
        mRecycler.addOnItemTouchListener(new RecyclerItemClickedListener(this,
                new RecyclerItemClickedListener.OnItemClickedListener() {
                    @Override
                    public void onItemClicked(View v, int postion) {
                        SharedPreferences sp = getSharedPreferences(MY_SP, Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putInt(EPISODE_POS, postion);
                        edit.putInt(EPISODE_ID, mEpisodes.get(postion).getId());
                        edit.putInt(SEASON_POS, mSpinner.getSelectedItemPosition());
                        edit.commit();

                        int id = mEpisodes.get(postion).getName();

                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        intent.putExtra("id", id);
                        intent.putExtra("title", mEpisodes.get(postion).getFullName());
                        startActivity(intent);
                    }
                }));

        mSpinner = (Spinner) findViewById(R.id.main_spinner);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadEpisodes(mSeasons.get(position).getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        try {
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);

            // Get private mPopup member variable and try cast to ListPopupWindow
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(mSpinner);

            // Set popupWindow height to 500px
            popupWindow.setHeight(500);
        } catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
            // silently fail...
        }

        mLoadingContainer = (RelativeLayout) findViewById(R.id.main_loading_container);
        mLoading = (AVLoadingIndicatorView) findViewById(R.id.main_loading);

        loadSeasons();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        mAdapter.notifyDataSetChanged();
        showLastVisit();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void showLoading(boolean mode) {
        if (mode) {
            mLoading.show();
            mLoadingContainer.setVisibility(View.VISIBLE);
        } else {
            mLoading.hide();
            mLoadingContainer.setVisibility(View.GONE);
        }
    }

    public void loadSeasons() {
        showLoading(true);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        mSeasonsCallback = new SeasonsCallback(this);
        api.getSeasons().enqueue(mSeasonsCallback);
    }

    public void onSeasonsLoadedSuccessfully(SeasonsResponse response) {
        mSeasons = response.getSeasonsDetail();

        ArrayList<String> spinnerData = new ArrayList<>();
        for (SeasonDetail season : mSeasons) {
            spinnerData.add(season.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerData);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        SharedPreferences sp = getSharedPreferences(MY_SP, Context.MODE_PRIVATE);
        int id = sp.getInt(SEASON_POS, 0);

        mSpinner.setSelection(id);
    }

    public void onSeasonsLoadedFailed() {
        mLoading.hide();
        Toast.makeText(this, "Khong the tai duoc du lieu", Toast.LENGTH_LONG).show();
        finish();
    }

    public void loadEpisodes(int seasonId) {
        showLoading(true);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        mEpisodesCallback = new EpisodesCallback(this);
        api.getEpisodes(seasonId).enqueue(mEpisodesCallback);
    }

    public void onEpisodesLoadedSuccessfully(EpisodesResponse response) {
        mEpisodes = response.getEpisodesDetail();

        if (mEpisodes.get(0).getName() == 0) {
            mEpisodes.remove(0);
        }

        mAdapter.setEpisodes(mEpisodes);
        mAdapter.notifyDataSetChanged();

        showLastVisit();
        showLoading(false);
    }

    public void onEpisodesLoadedFailed() {
        mLoading.hide();
        Toast.makeText(this, "Khong the tai duoc du lieu", Toast.LENGTH_LONG).show();
        finish();
    }

    public void showLastVisit() {
        SharedPreferences sp = getSharedPreferences(MY_SP, Context.MODE_PRIVATE);
        int epsPos = sp.getInt(EPISODE_POS, 0);
        int epsId = sp.getInt(EPISODE_ID, -1);
        int seaPos = sp.getInt(SEASON_POS, 0);

        if (epsId != -1) {
            if (seaPos == mSpinner.getSelectedItemPosition()) {
                mAdapter.setCurId(epsId);
                mRecycler.scrollToPosition(epsPos);
            } else {
                mAdapter.setCurId(-1);
                mRecycler.scrollToPosition(0);
            }
        } else {
            mRecycler.scrollToPosition(0);
        }
    }
}
