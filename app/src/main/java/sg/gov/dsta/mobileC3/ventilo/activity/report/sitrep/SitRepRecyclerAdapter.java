package sg.gov.dsta.mobileC3.ventilo.activity.report.sitrep;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sg.gov.dsta.mobileC3.ventilo.R;
import sg.gov.dsta.mobileC3.ventilo.model.sitrep.SitRepItemModel;
import sg.gov.dsta.mobileC3.ventilo.util.DateTimeUtil;

public class SitRepRecyclerAdapter extends RecyclerView.Adapter<SitRepViewHolder> {

    List<SitRepItemModel> mSitRepListItems;

    private Context mContext;

    public SitRepRecyclerAdapter(Context context, List<SitRepItemModel> sitRepListItems) {
        this.mContext = context;
        mSitRepListItems = sitRepListItems;
    }

    @Override
    public SitRepViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.recycler_view_row_sitrep, viewGroup, false);

        return new SitRepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SitRepViewHolder itemViewHolder, final int i) {
        SitRepItemModel item = mSitRepListItems.get(i);

        String reporter = "Reported By: ".concat(item.getReporter());
        itemViewHolder.getTvReporter().setText(reporter);
        itemViewHolder.getCircleImgAvatar().setImageDrawable(item.getReporterAvatar());

        String dateTimeString = "Reported Time: ".concat(DateTimeUtil.getTimeDifference(mContext, item.getReportedDateTime()));
        itemViewHolder.getTvDateTime().setText(dateTimeString);

//        itemViewHolder.get().setText(item.getCreatedBy());
//        itemViewHolder.getMNumCommentTV().setText(String.valueOf(item.getComments().size()));
////        itemViewHolder.getMTagTV().setText(item.get);
//        Date date = new Date();
//        itemViewHolder.getMTimeAgoTV().setText(DataAccess.timeAgo(date.getTime() - item.getCreatedDate().getTime()));
        // left images


//        itemViewHolder.mParentLinearLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "onClick:" + mTaskListItems.get(i));
//                Intent intent = new Intent(context, TasksViewActivity.class);
//                context.startActivity(intent);
//            }
//        });
    }

    public void addItem(String reporter, String location, String activity, int personnelT, int personnelS, int personnelD,
                        String nextCoa, String request) {
        SitRepItemModel newSitRepItemModel = new SitRepItemModel();
        newSitRepItemModel.setId(mSitRepListItems.size() + 1);
        newSitRepItemModel.setReporter(reporter);
        newSitRepItemModel.setReporterAvatar(mContext.getDrawable(R.drawable.default_soldier_icon));
        newSitRepItemModel.setLocation("BALESTIER");
        newSitRepItemModel.setActivity("Fire Fighting");
        newSitRepItemModel.setPersonnelT(6);
        newSitRepItemModel.setPersonnelS(5);
        newSitRepItemModel.setPersonnelD(4);
        newSitRepItemModel.setNextCOA("Inform HQ");
        newSitRepItemModel.setRequest("Additional MP");

        Date currentDateTime = Calendar.getInstance().getTime();
        newSitRepItemModel.setReportedDateTime(currentDateTime);

        mSitRepListItems.add(newSitRepItemModel);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mSitRepListItems.size();
    }

}
