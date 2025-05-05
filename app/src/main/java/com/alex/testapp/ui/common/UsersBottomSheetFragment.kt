import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.testapp.R
import com.alex.testapp.data.User
import com.alex.testapp.ui.adapter.UsersAdapter
import com.alex.testapp.ui.viewmodel.UserSelectionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UsersBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private var users: List<User> = emptyList()

    private lateinit var viewModel: UserSelectionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_users, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(UserSelectionViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup close button
        view.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dismiss()
        }

        recyclerUsers = view.findViewById(R.id.recycler_users)
        recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        usersAdapter = UsersAdapter()

        usersAdapter.setOnUserClickListener(object : UsersAdapter.OnUserClickListener {
            override fun onUserClick(user: User) {
                viewModel.selectUser(user)
                dismiss()
            }
        })

        recyclerUsers.adapter = usersAdapter
        usersAdapter.submitList(users)
    }

    companion object {
        const val TAG = "UsersBottomSheetFragment"

//        fun newInstance(): UsersBottomSheetFragment {
//            return UsersBottomSheetFragment()
//        }
        fun newInstance(users: List<User>): UsersBottomSheetFragment {
            val fragment = UsersBottomSheetFragment()
            fragment.users = users
            return fragment
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onBottomSheetDismissed()
    }

}