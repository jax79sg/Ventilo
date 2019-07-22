package sg.gov.dsta.mobileC3.ventilo.activity.radiolinkstatus;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sg.gov.dsta.mobileC3.ventilo.R;
import sg.gov.dsta.mobileC3.ventilo.model.user.UserModel;
import sg.gov.dsta.mobileC3.ventilo.model.viewmodel.UserViewModel;
import sg.gov.dsta.mobileC3.ventilo.util.StringUtil;
import sg.gov.dsta.mobileC3.ventilo.util.component.C2OpenSansLightTextView;
import sg.gov.dsta.mobileC3.ventilo.util.enums.radioLinkStatus.ERadioConnectionStatus;

public class RadioLinkStatusFragment extends Fragment {

    private static final String TAG = RadioLinkStatusFragment.class.getSimpleName();

    // View Models
    private UserViewModel mUserViewModel;

    // Main layout
    private View mRootView;

    // UI components
    private C2OpenSansLightTextView mTvOfflineTotal;
    private C2OpenSansLightTextView mTvOnlineTotal;

    // Recycler View
    private RecyclerView mRecyclerViewStatusOffline;
    private RadioLinkStatusOfflineRecyclerAdapter mRecyclerAdapterStatusOffline;
    private RecyclerView.LayoutManager mRecyclerLayoutManagerStatusOffline;

    private List<UserModel> mRadioLinkStatusUserListItems;
    private boolean mIsFragmentVisibleToUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);

        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_radio_link_status, container, false);
            observerSetup();
            initUI(mRootView);
        }

        return mRootView;
    }

    /**
     * Initialise view with recycler data
     *
     * @param rootView
     */
    private void initUI(View rootView) {
        mTvOfflineTotal = rootView.findViewById(R.id.tv_radio_link_status_offline_total);
        mTvOnlineTotal = rootView.findViewById(R.id.tv_radio_link_status_online_total);

        mRecyclerViewStatusOffline = rootView.findViewById(R.id.recycler_radio_status_link_offline);
        mRecyclerViewStatusOffline.setHasFixedSize(true);
        mRecyclerViewStatusOffline.setNestedScrollingEnabled(false);

        mRecyclerLayoutManagerStatusOffline = new LinearLayoutManager(getActivity());
        mRecyclerViewStatusOffline.setLayoutManager(mRecyclerLayoutManagerStatusOffline);

        if (mRadioLinkStatusUserListItems == null) {
            mRadioLinkStatusUserListItems = new ArrayList<>();
        }

        mRecyclerAdapterStatusOffline = new RadioLinkStatusOfflineRecyclerAdapter(getContext(),
                mRadioLinkStatusUserListItems);
        mRecyclerViewStatusOffline.setAdapter(mRecyclerAdapterStatusOffline);
        mRecyclerViewStatusOffline.setItemAnimator(new DefaultItemAnimator());
    }

    /**
     * Refresh UI with updated data
     */
    private void refreshUI() {
        if (mRadioLinkStatusUserListItems != null) {
            // Extracts list of radio connection status from UserModel
            List<String> radioConnectionStatusList = mRadioLinkStatusUserListItems.stream().map(
                UserModel -> UserModel.getRadioFullConnectionStatus()).collect(Collectors.toList());

            // Get count of number of users offline and online before displaying count
            long noOfUsersOffline = radioConnectionStatusList.stream().filter(status ->
                    status.equalsIgnoreCase(ERadioConnectionStatus.OFFLINE.toString())).count();
            long noOfUsersOnline = radioConnectionStatusList.stream().filter(status ->
                    status.equalsIgnoreCase(ERadioConnectionStatus.ONLINE.toString())).count();

            String offlineTotal = String.valueOf(noOfUsersOffline).concat(StringUtil.FORWARD_SLASH).
                    concat(String.valueOf(mRadioLinkStatusUserListItems.size()));
            String onlineTotal = String.valueOf(noOfUsersOnline).concat(StringUtil.FORWARD_SLASH).
                    concat(String.valueOf(mRadioLinkStatusUserListItems.size()));

            mTvOfflineTotal.setText(offlineTotal);
            mTvOnlineTotal.setText(onlineTotal);
        }
    }

    /**
     * Set up observer for live updates on view models and update UI accordingly
     */
    private void observerSetup() {
        mUserViewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        /*
         * Refreshes recyclerview UI whenever there is a change in user data (insert, update or delete)
         */
        mUserViewModel.getAllUsersLiveData().observe(this, new Observer<List<UserModel>>() {
            @Override
            public void onChanged(@Nullable List<UserModel> userModelList) {
                if (mRadioLinkStatusUserListItems == null) {
                    mRadioLinkStatusUserListItems = new ArrayList<>();
                } else {
                    mRadioLinkStatusUserListItems.clear();
                }

                if (userModelList != null) {
                    mRadioLinkStatusUserListItems.addAll(userModelList);
                }

                if (mRecyclerAdapterStatusOffline != null) {
                    mRecyclerAdapterStatusOffline.setUserListItems(mRadioLinkStatusUserListItems);
                }

                refreshUI();
            }
        });
    }

    /**
     * Pops back stack of ONLY current tab
     *
     * @return
     */
    public boolean popBackStack() {
        if (!isAdded())
            return false;

        if(getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStackImmediate();
            return true;
        } else
            return false;
    }

    private void onVisible() {
        Log.d(TAG, "onVisible");
    }

    private void onInvisible() {
        Log.d(TAG, "onInvisible");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsFragmentVisibleToUser = isVisibleToUser;

        if (isResumed()) { // fragment has been created at this point
            if (mIsFragmentVisibleToUser) {
                Log.d(TAG, "setUserVisibleHint onVisible");
                onVisible();
            } else {
                Log.d(TAG, "setUserVisibleHint onInvisible");
                onInvisible();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mIsFragmentVisibleToUser) {
            onVisible();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mIsFragmentVisibleToUser) {
            onInvisible();
        }
    }
}
