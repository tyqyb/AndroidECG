package USTB.AAIST.adapter;
//设备列表适配器
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import USTB.AAIST.R;
import USTB.AAIST.devicesdata.Devices;

public class DevicesAdapterList extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private final List<Devices> devices;

    public DevicesAdapterList(Context context, List<Devices> devices) {
        this.devices = devices;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.devices_list_inble, null);//devices_list_inble布局文件
            viewHolder = new ViewHolder();
            viewHolder.mTvAddress = convertView.findViewById(R.id.devices_address);//devices_list_inble布局文件中显示设备列表地址
            viewHolder.mTvName = convertView.findViewById(R.id.devices_name);//devices_list_inble布局文件中显示设备列表名称
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.mTvName.setText(devices.get(position).getName());
        viewHolder.mTvAddress.setText(devices.get(position).getAddress());
        return convertView;
    }

    static class ViewHolder {
        TextView mTvName, mTvAddress;
    }
}
